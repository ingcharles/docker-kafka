package com.ecommerce.notification.infrastructure.messaging.producer;

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

import com.ecommerce.notification.infrastructure.outbox.OutboxEventEntity;
import com.ecommerce.notification.infrastructure.outbox.OutboxEventRepository;

/**
 * Revisa periódicamente la tabla {@code outbox_event} en busca de filas que
 * todavía no se hayan publicado, y las reenvía a Kafka. Se usa {@code
 * aggregateId} (que es el {@code orderId}) como key de Kafka, para que todos
 * los mensajes de una misma orden caigan siempre en la misma partición y se
 * mantengan en orden entre sí. Corre cada segundo; el tamaño de lote está
 * acotado para que una sola ejecución no bloquee la siguiente.
 */
@ApplicationScoped
public class OutboxRelay {

    private static final Logger LOG = Logger.getLogger(OutboxRelay.class);
    private static final int BATCH_SIZE = 20;

    @Inject
    OutboxEventRepository outboxRepository;

    @Inject
    @Channel("notification-events-out")
    Emitter<String> notificationEventsEmitter;

    @Scheduled(every = "1s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    @WithTransaction
    public Uni<Void> relay() {
        return outboxRepository.findUnpublishedBatch(BATCH_SIZE)
            .onItem().transformToMulti(batch -> Multi.createFrom().iterable(batch))
            .onItem().transformToUniAndConcatenate(this::publish)
            .collect().last()
            .replaceWithVoid();
    }

    private Uni<OutboxEventEntity> publish(OutboxEventEntity event) {
        // Emitter#send(Message) es "fire-and-forget" (retorna void); para saber cuándo
        // el productor de Kafka realmente confirmó la escritura, le pasamos nuestros
        // propios callbacks de ack/nack y completamos un CompletableFuture desde ahí —
        // el conector de Kafka de SmallRye los invoca después de que el broker responde,
        // pero lo hace en el hilo de red propio del cliente de Kafka, NO en el contexto
        // de Vert.x que es dueño de la sesión actual de Hibernate Reactive. El emitOn()
        // de abajo salta de vuelta a ese contexto antes de volver a tocar `event`:
        // Hibernate Reactive lanza una excepción (HR000069) si su sesión se toca desde
        // el hilo equivocado.
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
        notificationEventsEmitter.send(message);

        return Uni.createFrom().completionStage((CompletionStage<Void>) delivered)
            .emitOn(runnable -> callingContext.runOnContext(ignored -> runnable.run()))
            .onItem().invoke(() -> event.publishedAt = Instant.now())
            .replaceWith(event)
            .onFailure().invoke(failure -> LOG.errorf(failure,
                "Failed to publish outbox event %s to Kafka, will retry next tick", event.id))
            .onFailure().recoverWithItem(event);
    }
}
