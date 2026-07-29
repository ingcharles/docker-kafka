package com.facturacion.firma.infrastructure.messaging.producer;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.jboss.logging.Logger;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import io.vertx.core.Vertx;

import com.facturacion.firma.infrastructure.outbox.OutboxEventEntity;
import com.facturacion.firma.infrastructure.outbox.OutboxEventRepository;
import com.facturacion.firma.shared.constant.MessagingConstants;

/**
 * A diferencia del relay de outbox de un solo tópico fijo (como en la
 * mayoría de los servicios de ecommerce/), este servicio produce hacia
 * <b>dos</b> tópicos: eventos de empresa ({@code EmpresaService}) y eventos
 * de firma ({@code FirmarComprobanteConsumer}) — así que enruta cada fila
 * según {@code outbox_event.topic}, mismo patrón que {@code SagaCommandRelay}
 * en saga-orchestrator-service. Corre cada segundo; es la pieza que
 * satisface el requisito regulatorio de "transmisión inmediata" (Resolución
 * NAC-DGERCGC25-00000017, vigente desde 2026-01-01).
 */
@ApplicationScoped
public class OutboxRelay {

    private static final Logger LOG = Logger.getLogger(OutboxRelay.class);
    private static final int BATCH_SIZE = 20;

    @Inject
    OutboxEventRepository outboxRepository;

    @Inject
    @Channel("empresa-events-out")
    Emitter<String> empresaEventsEmitter;

    @Inject
    @Channel("firma-events-out")
    Emitter<String> firmaEventsEmitter;

    @Inject
    @Channel("establecimiento-events-out")
    Emitter<String> establecimientoEventsEmitter;

    @Inject
    @Channel("configuracion-events-out")
    Emitter<String> configuracionEventsEmitter;

    private Map<String, Emitter<String>> emittersByTopic;

    @jakarta.annotation.PostConstruct
    void init() {
        emittersByTopic = Map.of(
            MessagingConstants.EMPRESA_EVENTS_TOPIC, empresaEventsEmitter,
            MessagingConstants.FIRMA_EVENTS_TOPIC, firmaEventsEmitter,
            MessagingConstants.ESTABLECIMIENTO_PUNTO_EMISION_EVENTS_TOPIC, establecimientoEventsEmitter,
            MessagingConstants.CONFIGURACION_EMPRESA_EVENTS_TOPIC, configuracionEventsEmitter);
    }

    // "delayed" evita una carrera de arranque real: el primer tick del
    // scheduler puede disparar antes de que SmallRye Reactive Messaging
    // termine de conectar los emitters de Kafka, lo que producía un
    // DefinitionException una sola vez en cada arranque (se autocorregía
    // en el siguiente tick, pero quedaba un ERROR falso en el log).
    @Scheduled(every = "1s", delayed = "2s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    @WithTransaction
    public Uni<Void> relay() {
        return outboxRepository.findUnpublishedBatch(BATCH_SIZE)
            .onItem().transformToMulti(batch -> Multi.createFrom().iterable(batch))
            .onItem().transformToUniAndConcatenate(this::publish)
            .collect().last()
            .replaceWithVoid();
    }

    private Uni<OutboxEventEntity> publish(OutboxEventEntity event) {
        Emitter<String> emitter = emittersByTopic.get(event.topic);
        if (emitter == null) {
            LOG.errorf("No hay emitter configurado para el tópico %s (outbox event %s) — se reintenta luego",
                event.topic, event.id);
            return Uni.createFrom().item(event);
        }

        io.vertx.core.Context callingContext = Vertx.currentContext();
        CompletableFuture<Void> delivered = new CompletableFuture<>();
        Message<String> message = Message.of(event.payload,
                Metadata.of(OutgoingKafkaRecordMetadata.<String>builder()
                    .withKey(event.aggregateId.toString())
                    .build()))
            .withAck(() -> {
                delivered.complete(null);
                return CompletableFuture.completedFuture(null);
            })
            .withNack(reason -> {
                delivered.completeExceptionally(reason);
                return CompletableFuture.completedFuture(null);
            });
        emitter.send(message);

        return Uni.createFrom().completionStage((CompletionStage<Void>) delivered)
            .emitOn(runnable -> callingContext.runOnContext(ignored -> runnable.run()))
            .onItem().invoke(() -> event.publishedAt = Instant.now())
            .replaceWith(event)
            .onFailure().invoke(failure -> LOG.errorf(failure,
                "No se pudo publicar el evento de outbox %s (tópico %s) en Kafka, se reintenta en el siguiente tick",
                event.id, event.topic))
            .onFailure().recoverWithItem(event);
    }
}
