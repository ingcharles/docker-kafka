package com.facturacion.orquestacion.infrastructure.messaging.producer;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.annotation.PostConstruct;
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
import io.vertx.core.Context;
import io.vertx.core.Vertx;

import com.facturacion.orquestacion.infrastructure.outbox.OutboxEventEntity;
import com.facturacion.orquestacion.infrastructure.outbox.OutboxEventRepository;
import com.facturacion.orquestacion.shared.constant.MessagingConstants;

/**
 * A diferencia del relay de outbox de los demás servicios del stack (atado
 * a un único tópico fijo), este enruta cada fila hacia uno de tres tópicos
 * de comando distintos según {@code outbox_event.topic} — el orquestador es
 * el único participante que emite hacia más de un destino (mismo patrón que
 * {@code SagaCommandRelay} en ecommerce/saga-orchestrator-service, y que
 * {@code OutboxRelay} de inventario-service para sus dos tópicos). "delayed"
 * evita la carrera de arranque con los emitters de Reactive Messaging —
 * acá hace más falta que en otros servicios: son 3 canales salientes más 4 entrantes.
 */
@ApplicationScoped
public class OrquestacionCommandRelay {

    private static final Logger LOG = Logger.getLogger(OrquestacionCommandRelay.class);
    private static final int BATCH_SIZE = 20;

    @Inject
    OutboxEventRepository outboxRepository;

    @Inject
    @Channel("comprobante-commands-out")
    Emitter<String> comprobanteCommandsEmitter;

    @Inject
    @Channel("firma-commands-out")
    Emitter<String> firmaCommandsEmitter;

    @Inject
    @Channel("sri-commands-out")
    Emitter<String> sriCommandsEmitter;

    private Map<String, Emitter<String>> emittersByTopic;

    @PostConstruct
    void init() {
        emittersByTopic = Map.of(
            MessagingConstants.COMPROBANTE_COMMANDS_TOPIC, comprobanteCommandsEmitter,
            MessagingConstants.FIRMA_COMMANDS_TOPIC, firmaCommandsEmitter,
            MessagingConstants.SRI_COMMANDS_TOPIC, sriCommandsEmitter);
    }

    @Scheduled(every = "1s", delayed = "3s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
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
            LOG.errorf("No hay emitter configurado para el tópico %s (evento de outbox %s) — se omite, se reintenta",
                event.topic, event.id);
            return Uni.createFrom().item(event);
        }

        Context callingContext = Vertx.currentContext();
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
            // Sin ack en 30 s (broker caído): se suelta la transacción y sus locks en vez de colgar el relay; se reintenta en el siguiente tick.
            .ifNoItem().after(java.time.Duration.ofSeconds(30)).fail()
            .emitOn(runnable -> callingContext.runOnContext(ignored -> runnable.run()))
            .onItem().invoke(() -> event.publishedAt = Instant.now())
            .replaceWith(event)
            .onFailure().invoke(failure -> LOG.errorf(failure,
                "No se pudo publicar el evento de outbox %s (tópico %s) en Kafka, se reintenta en el siguiente tick",
                event.id, event.topic))
            .onFailure().recoverWithItem(event);
    }
}
