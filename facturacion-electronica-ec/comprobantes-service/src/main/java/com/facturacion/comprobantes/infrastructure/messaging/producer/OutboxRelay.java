package com.facturacion.comprobantes.infrastructure.messaging.producer;

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

import com.facturacion.comprobantes.infrastructure.outbox.OutboxEventEntity;
import com.facturacion.comprobantes.infrastructure.outbox.OutboxEventRepository;

/**
 * Revisa {@code outbox_event} cada segundo y reenvía a Kafka lo pendiente,
 * usando {@code aggregateId} (el {@code comprobanteId}) como key — es la
 * pieza que satisface "transmisión inmediata" (Resolución NAC-DGERCGC25-00000017,
 * vigente desde 2026-01-01): nada queda encolado más de ~1 segundo. El
 * "delayed" evita la carrera de arranque con los emitters de Reactive
 * Messaging documentada en la memoria de gotchas del proyecto.
 */
@ApplicationScoped
public class OutboxRelay {

    private static final Logger LOG = Logger.getLogger(OutboxRelay.class);
    private static final int BATCH_SIZE = 20;

    @Inject
    OutboxEventRepository outboxRepository;

    @Inject
    @Channel("comprobante-events-out")
    Emitter<String> comprobanteEventsEmitter;

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
        comprobanteEventsEmitter.send(message);

        return Uni.createFrom().completionStage((CompletionStage<Void>) delivered)
            .emitOn(runnable -> callingContext.runOnContext(ignored -> runnable.run()))
            .onItem().invoke(() -> event.publishedAt = Instant.now())
            .replaceWith(event)
            .onFailure().invoke(failure -> LOG.errorf(failure,
                "No se pudo publicar el evento de outbox %s en Kafka, se reintenta en el siguiente tick", event.id))
            .onFailure().recoverWithItem(event);
    }
}
