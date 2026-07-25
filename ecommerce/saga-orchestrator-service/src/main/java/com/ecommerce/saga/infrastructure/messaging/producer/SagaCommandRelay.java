package com.ecommerce.saga.infrastructure.messaging.producer;

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

import com.ecommerce.saga.infrastructure.outbox.OutboxEventEntity;
import com.ecommerce.saga.infrastructure.outbox.OutboxEventRepository;
import com.ecommerce.saga.shared.constant.MessagingConstants;

/**
 * A diferencia del relay de outbox de todos los demás servicios (que está
 * atado a un único tópico fijo), este enruta cada fila hacia uno de seis
 * tópicos distintos, según el valor de {@code outbox_event.topic} — el
 * orquestador es el único participante que emite hacia más de un destino.
 * Fuera de eso, es idéntico al resto: usa {@code aggregateId} (el orderId)
 * como key, y hace el mismo salto de contexto de Vert.x antes de volver a
 * tocar el estado de Hibernate Reactive (ver el {@code OutboxRelay} de
 * order-service para la explicación completa de por qué es necesario ese
 * salto).
 */
@ApplicationScoped
public class SagaCommandRelay {

    private static final Logger LOG = Logger.getLogger(SagaCommandRelay.class);
    private static final int BATCH_SIZE = 20;

    @Inject
    OutboxEventRepository outboxRepository;

    @Inject
    @Channel("order-commands-out")
    Emitter<String> orderCommandsEmitter;

    @Inject
    @Channel("inventory-commands-out")
    Emitter<String> inventoryCommandsEmitter;

    @Inject
    @Channel("payment-commands-out")
    Emitter<String> paymentCommandsEmitter;

    @Inject
    @Channel("shipping-commands-out")
    Emitter<String> shippingCommandsEmitter;

    @Inject
    @Channel("notification-commands-out")
    Emitter<String> notificationCommandsEmitter;

    @Inject
    @Channel("saga-events-out")
    Emitter<String> sagaEventsEmitter;

    private Map<String, Emitter<String>> emittersByTopic;

    @PostConstruct
    void init() {
        emittersByTopic = Map.of(
            MessagingConstants.ORDER_COMMANDS_TOPIC, orderCommandsEmitter,
            MessagingConstants.INVENTORY_COMMANDS_TOPIC, inventoryCommandsEmitter,
            MessagingConstants.PAYMENT_COMMANDS_TOPIC, paymentCommandsEmitter,
            MessagingConstants.SHIPPING_COMMANDS_TOPIC, shippingCommandsEmitter,
            MessagingConstants.NOTIFICATION_COMMANDS_TOPIC, notificationCommandsEmitter,
            MessagingConstants.SAGA_EVENTS_TOPIC, sagaEventsEmitter);
    }

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
        Emitter<String> emitter = emittersByTopic.get(event.topic);
        if (emitter == null) {
            LOG.errorf("No emitter configured for topic %s (outbox event %s) — skipping, will retry",
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
            .emitOn(runnable -> callingContext.runOnContext(ignored -> runnable.run()))
            .onItem().invoke(() -> event.publishedAt = Instant.now())
            .replaceWith(event)
            .onFailure().invoke(failure -> LOG.errorf(failure,
                "Failed to publish outbox event %s (topic %s) to Kafka, will retry next tick",
                event.id, event.topic))
            .onFailure().recoverWithItem(event);
    }
}
