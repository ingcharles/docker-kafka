package com.ecommerce.order.infrastructure.messaging.consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.ecommerce.order.application.event.OrderCommandMessage;
import com.ecommerce.order.domain.model.enums.OrderStatus;
import com.ecommerce.order.domain.repository.OrderRepository;
import com.ecommerce.order.infrastructure.inbox.ProcessedMessageEntity;
import com.ecommerce.order.infrastructure.inbox.ProcessedMessageRepository;
import com.ecommerce.order.shared.constant.MessagingConstants;

/**
 * Aplica los dos comandos que el orquestador de la saga envía una vez que ya
 * decidió cómo termina una orden: {@code ConfirmOrder} en el camino feliz, o
 * {@code CancelOrder} en cualquiera de los caminos de compensación. Es
 * idempotente gracias al inbox ({@code processed_message}) — como Kafka
 * garantiza entrega "al menos una vez", este manejador puede llegar a
 * recibir el mismo {@code eventId} más de una vez, y debe manejarlo sin
 * aplicar el efecto dos veces.
 */
@ApplicationScoped
public class OrderCommandConsumer {

    private static final Logger LOG = Logger.getLogger(OrderCommandConsumer.class);

    private final OrderRepository orderRepository;
    private final ProcessedMessageRepository processedMessageRepository;

    @Inject
    public OrderCommandConsumer(OrderRepository orderRepository,
        ProcessedMessageRepository processedMessageRepository) {
        this.orderRepository = orderRepository;
        this.processedMessageRepository = processedMessageRepository;
    }

    @Incoming("order-commands-in")
    @WithTransaction
    public Uni<Void> onCommand(OrderCommandMessage command) {
        return processedMessageRepository.alreadyProcessed(command.eventId())
            .onItem().transformToUni(processed -> {
                if (Boolean.TRUE.equals(processed)) {
                    LOG.infof("Skipping already-processed command %s for order %s",
                        command.eventId(), command.orderId());
                    return Uni.createFrom().voidItem();
                }
                return apply(command)
                    .chain(() -> processedMessageRepository.persist(
                        ProcessedMessageEntity.of(command.eventId())))
                    .replaceWithVoid();
            });
    }

    private Uni<Void> apply(OrderCommandMessage command) {
        return switch (command.commandType()) {
            case MessagingConstants.COMMAND_TYPE_CONFIRM_ORDER -> orderRepository
                .updateStatus(command.orderId(), OrderStatus.CONFIRMED, OrderStatus.PENDING)
                .replaceWithVoid();
            case MessagingConstants.COMMAND_TYPE_CANCEL_ORDER -> orderRepository
                .updateStatus(command.orderId(), OrderStatus.CANCELLED, OrderStatus.PENDING)
                .replaceWithVoid();
            default -> {
                LOG.warnf("Unknown order command type: %s", command.commandType());
                yield Uni.createFrom().voidItem();
            }
        };
    }
}
