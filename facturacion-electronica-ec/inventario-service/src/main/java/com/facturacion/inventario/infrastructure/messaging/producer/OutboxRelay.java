package com.facturacion.inventario.infrastructure.messaging.producer;

import java.time.Instant;
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

import com.facturacion.commons.error.LogCodificado;
import com.facturacion.inventario.infrastructure.outbox.OutboxEventEntity;
import com.facturacion.inventario.infrastructure.outbox.OutboxEventRepository;
import com.facturacion.inventario.shared.constant.MessagingConstants;
import com.facturacion.inventario.shared.error.CodigoLog;

/**
 * Revisa periódicamente la tabla {@code outbox_event} y reenvía a Kafka lo
 * pendiente, usando {@code aggregateId} (el {@code comprobanteId}/{@code itemId}
 * del evento) como key — mismo mecanismo que en firma-electronica-service,
 * con el ajuste de "delayed" para evitar la carrera de arranque con los
 * emitters de Reactive Messaging. Este servicio publica en dos tópicos
 * distintos (stock y catálogo) desde la misma tabla de outbox, así que el
 * emitter a usar se elige según {@code event.topic} — a diferencia del
 * resto de servicios del stack, que hasta ahora solo tenían un tópico de
 * salida y podían asumir un único emitter fijo.
 */
@ApplicationScoped
public class OutboxRelay {

    private static final Logger LOG = Logger.getLogger(OutboxRelay.class);
    private static final int BATCH_SIZE = 20;

    @Inject
    OutboxEventRepository outboxRepository;

    @Inject
    @Channel("inventario-events-out")
    Emitter<String> inventarioEventsEmitter;

    @Inject
    @Channel("item-events-out")
    Emitter<String> itemEventsEmitter;

    @Scheduled(every = "1s", delayed = "3s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    @WithTransaction
    public Uni<Void> publicarEventosPendientes() {
        return outboxRepository.findUnpublishedBatch(BATCH_SIZE)
            .onItem().transformToMulti(batch -> Multi.createFrom().iterable(batch))
            .onItem().transformToUniAndConcatenate(this::publish)
            .collect().last()
            .replaceWithVoid();
    }

    private Emitter<String> obtenerEmisorPara(String topic) {
        return MessagingConstants.ITEM_EVENTS_TOPIC.equals(topic) ? itemEventsEmitter : inventarioEventsEmitter;
    }

    private Uni<OutboxEventEntity> publish(OutboxEventEntity event) {
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
        obtenerEmisorPara(event.topic).send(message);

        return Uni.createFrom().completionStage((CompletionStage<Void>) delivered)
            // Sin ack en 30 s (broker caído): se suelta la transacción y sus locks en vez de colgar el relay; se reintenta en el siguiente tick.
            .ifNoItem().after(java.time.Duration.ofSeconds(30)).fail()
            .emitOn(runnable -> callingContext.runOnContext(ignored -> runnable.run()))
            .onItem().invoke(() -> event.publishedAt = Instant.now())
            .replaceWith(event)
            .onFailure().invoke(failure -> LogCodificado.error(LOG, failure, CodigoLog.OUTBOX_PUBLICACION_FALLIDA, "eventoId=" + event.id))
            .onFailure().recoverWithItem(event);
    }
}
