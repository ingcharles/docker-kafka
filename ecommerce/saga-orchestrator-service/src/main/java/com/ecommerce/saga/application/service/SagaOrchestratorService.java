package com.ecommerce.saga.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.ecommerce.saga.application.event.AddressPayload;
import com.ecommerce.saga.application.event.InventoryCommandMessage;
import com.ecommerce.saga.application.event.InventoryEventMessage;
import com.ecommerce.saga.application.event.InventoryItemPayload;
import com.ecommerce.saga.application.event.NotificationCommandMessage;
import com.ecommerce.saga.application.event.NotificationEventMessage;
import com.ecommerce.saga.application.event.OrderCommandMessage;
import com.ecommerce.saga.application.event.OrderEventMessage;
import com.ecommerce.saga.application.event.OrderItemPayload;
import com.ecommerce.saga.application.event.PaymentCommandMessage;
import com.ecommerce.saga.application.event.PaymentEventMessage;
import com.ecommerce.saga.application.event.SagaAuditMessage;
import com.ecommerce.saga.application.event.ShippingCommandMessage;
import com.ecommerce.saga.application.event.ShippingEventMessage;
import com.ecommerce.saga.domain.model.entity.SagaInstance;
import com.ecommerce.saga.domain.model.entity.SagaStep;
import com.ecommerce.saga.domain.model.enums.SagaState;
import com.ecommerce.saga.domain.model.vo.Address;
import com.ecommerce.saga.domain.repository.SagaInstanceRepository;
import com.ecommerce.saga.domain.repository.SagaStepRepository;
import com.ecommerce.saga.infrastructure.inbox.ProcessedMessageEntity;
import com.ecommerce.saga.infrastructure.inbox.ProcessedMessageRepository;
import com.ecommerce.saga.infrastructure.outbox.OutboxEventEntity;
import com.ecommerce.saga.infrastructure.outbox.OutboxEventRepository;
import com.ecommerce.saga.shared.constant.MessagingConstants;

/**
 * La única pieza de todo el sistema que conoce la saga completa. Todos los
 * manejadores siguen la misma estructura: revisar el inbox, aplicar la
 * transición de estado (que no hace nada si la saga no está en el estado de
 * origen esperado — por ejemplo, una entrega duplicada después de un
 * reintento), emitir lo que corresponda a continuación, y marcar el inbox.
 * Ver README.md para la tabla completa de transiciones/compensaciones que
 * aquí se implementa.
 */
@ApplicationScoped
public class SagaOrchestratorService {

    private final SagaInstanceRepository sagaInstanceRepository;
    private final SagaStepRepository sagaStepRepository;
    private final OutboxEventRepository outboxRepository;
    private final ProcessedMessageRepository processedMessageRepository;
    private final ObjectMapper objectMapper;

    @Inject
    public SagaOrchestratorService(SagaInstanceRepository sagaInstanceRepository,
        SagaStepRepository sagaStepRepository, OutboxEventRepository outboxRepository,
        ProcessedMessageRepository processedMessageRepository, ObjectMapper objectMapper) {
        this.sagaInstanceRepository = sagaInstanceRepository;
        this.sagaStepRepository = sagaStepRepository;
        this.outboxRepository = outboxRepository;
        this.processedMessageRepository = processedMessageRepository;
        this.objectMapper = objectMapper;
    }

    @WithTransaction
    public Uni<Void> handleOrderCreated(OrderEventMessage event) {
        return ifNotProcessed(event.eventId(), () -> {
            Address address = new Address(event.shippingAddress().street(), event.shippingAddress().city(),
                event.shippingAddress().state(), event.shippingAddress().zip(),
                event.shippingAddress().country());
            SagaInstance saga = SagaInstance.start(event.orderId(), event.customerId(), event.totalAmount(),
                event.currency(), address, writeValueAsString(event.items()));

            List<InventoryItemPayload> reserveItems = event.items().stream()
                .map(item -> new InventoryItemPayload(item.productId(), item.quantity()))
                .collect(Collectors.toList());
            InventoryCommandMessage command = new InventoryCommandMessage(UUID.randomUUID(),
                event.orderId(), MessagingConstants.COMMAND_TYPE_RESERVE_STOCK, reserveItems, Instant.now());

            return sagaInstanceRepository.persist(saga)
                .chain(() -> logReceived(event.orderId(), MessagingConstants.EVENT_TYPE_ORDER_CREATED))
                .chain(() -> emit(event.orderId(), MessagingConstants.COMMAND_TYPE_RESERVE_STOCK,
                    MessagingConstants.INVENTORY_COMMANDS_TOPIC, command))
                .chain(() -> markProcessed(event.eventId()));
        });
    }

    @WithTransaction
    public Uni<Void> handleInventoryEvent(InventoryEventMessage event) {
        return ifNotProcessed(event.eventId(), () -> logReceived(event.orderId(), event.eventType())
            .chain(() -> switch (event.eventType()) {
                case MessagingConstants.EVENT_TYPE_STOCK_RESERVED -> onStockReserved(event.orderId());
                case MessagingConstants.EVENT_TYPE_STOCK_RESERVATION_FAILED ->
                    onStockReservationFailed(event.orderId(), event.reason());
                // StockReleased: solo queda registrado en la bitácora, no dispara ninguna transición.
                default -> Uni.createFrom().voidItem();
            })
            .chain(() -> markProcessed(event.eventId())));
    }

    @WithTransaction
    public Uni<Void> handlePaymentEvent(PaymentEventMessage event) {
        return ifNotProcessed(event.eventId(), () -> logReceived(event.orderId(), event.eventType())
            .chain(() -> switch (event.eventType()) {
                case MessagingConstants.EVENT_TYPE_PAYMENT_APPROVED -> onPaymentApproved(event.orderId());
                case MessagingConstants.EVENT_TYPE_PAYMENT_DECLINED ->
                    onPaymentDeclined(event.orderId(), event.reason());
                // PaymentRefunded: solo queda registrado en la bitácora, no dispara ninguna transición.
                default -> Uni.createFrom().voidItem();
            })
            .chain(() -> markProcessed(event.eventId())));
    }

    @WithTransaction
    public Uni<Void> handleShippingEvent(ShippingEventMessage event) {
        return ifNotProcessed(event.eventId(), () -> logReceived(event.orderId(), event.eventType())
            .chain(() -> switch (event.eventType()) {
                case MessagingConstants.EVENT_TYPE_SHIPMENT_CREATED -> onShipmentCreated(event.orderId());
                case MessagingConstants.EVENT_TYPE_SHIPMENT_FAILED ->
                    onShipmentFailed(event.orderId(), event.reason());
                // ShipmentCancelled: solo queda registrado en la bitácora; hoy no se usa realmente.
                default -> Uni.createFrom().voidItem();
            })
            .chain(() -> markProcessed(event.eventId())));
    }

    @WithTransaction
    public Uni<Void> handleNotificationEvent(NotificationEventMessage event) {
        return ifNotProcessed(event.eventId(), () -> logReceived(event.orderId(), event.eventType())
            .chain(() -> onNotificationSettled(event.orderId(), event.eventType(), event.reason()))
            .chain(() -> markProcessed(event.eventId())));
    }

    /**
     * Vuelve a publicar el comando enviado más recientemente (estado SENT)
     * para esta saga, usando exactamente el mismo contenido — y por lo
     * tanto el mismo {@code eventId} incrustado — que se emitió la primera
     * vez. Es la idempotencia del servicio destino la que después decide si
     * esto es un reintento genuino o si no hace falta nada. Se usa para
     * recuperar una saga que quedó atascada por un comando que se perdió.
     */
    @WithTransaction
    public Uni<Void> retryLastCommand(UUID orderId) {
        return sagaStepRepository.findLastSent(orderId)
            .onItem().transformToUni(lastSent -> {
                if (lastSent == null) {
                    return Uni.createFrom().voidItem();
                }
                return outboxRepository.persist(OutboxEventEntity.of(orderId, lastSent.getStepName(),
                        lastSent.getTopic(), lastSent.getPayload()))
                    .replaceWithVoid();
            });
    }

    // --- Camino feliz -----------------------------------------------------

    private Uni<Void> onStockReserved(UUID orderId) {
        return sagaInstanceRepository.updateState(orderId, SagaState.STOCK_RESERVED, null, SagaState.STARTED)
            .onItem().transformToUni(saga -> {
                if (saga == null) {
                    return Uni.createFrom().voidItem();
                }
                PaymentCommandMessage command = new PaymentCommandMessage(UUID.randomUUID(), orderId,
                    MessagingConstants.COMMAND_TYPE_CHARGE_PAYMENT, saga.getTotalAmount(),
                    saga.getCurrency(), Instant.now());
                return emit(orderId, MessagingConstants.COMMAND_TYPE_CHARGE_PAYMENT,
                    MessagingConstants.PAYMENT_COMMANDS_TOPIC, command);
            });
    }

    private Uni<Void> onPaymentApproved(UUID orderId) {
        return sagaInstanceRepository.updateState(orderId, SagaState.PAYMENT_APPROVED, null,
                SagaState.STOCK_RESERVED)
            .onItem().transformToUni(saga -> {
                if (saga == null) {
                    return Uni.createFrom().voidItem();
                }
                ShippingCommandMessage command = new ShippingCommandMessage(UUID.randomUUID(), orderId,
                    MessagingConstants.COMMAND_TYPE_CREATE_SHIPMENT, toAddressPayload(saga.getShippingAddress()),
                    Instant.now());
                return emit(orderId, MessagingConstants.COMMAND_TYPE_CREATE_SHIPMENT,
                    MessagingConstants.SHIPPING_COMMANDS_TOPIC, command);
            });
    }

    private Uni<Void> onShipmentCreated(UUID orderId) {
        return sagaInstanceRepository.updateState(orderId, SagaState.SHIPMENT_CREATED, null,
                SagaState.PAYMENT_APPROVED)
            .onItem().transformToUni(saga -> {
                if (saga == null) {
                    return Uni.createFrom().voidItem();
                }
                NotificationCommandMessage command = new NotificationCommandMessage(UUID.randomUUID(),
                    orderId, MessagingConstants.COMMAND_TYPE_SEND_NOTIFICATION,
                    MessagingConstants.NOTIFICATION_TYPE_ORDER_CONFIRMED,
                    saga.getCustomerId() + "@example.com", Instant.now());
                return emit(orderId, MessagingConstants.COMMAND_TYPE_SEND_NOTIFICATION,
                    MessagingConstants.NOTIFICATION_COMMANDS_TOPIC, command);
            });
    }

    /**
     * Ambos resultados (éxito y fallo) terminan aquí: una notificación
     * fallida nunca bloquea que la saga se complete — para este punto la
     * orden ya está pagada y despachada, así que no queda absolutamente
     * nada que compensar.
     */
    private Uni<Void> onNotificationSettled(UUID orderId, String eventType, String reason) {
        return sagaInstanceRepository.updateState(orderId, SagaState.COMPLETED,
                MessagingConstants.EVENT_TYPE_NOTIFICATION_FAILED.equals(eventType)
                    ? "Notification failed but order was already paid/shipped: " + reason
                    : null,
                SagaState.SHIPMENT_CREATED)
            .onItem().transformToUni(saga -> {
                if (saga == null) {
                    return Uni.createFrom().voidItem();
                }
                OrderCommandMessage command = new OrderCommandMessage(UUID.randomUUID(), orderId,
                    MessagingConstants.COMMAND_TYPE_CONFIRM_ORDER, null, Instant.now());
                return emit(orderId, MessagingConstants.COMMAND_TYPE_CONFIRM_ORDER,
                    MessagingConstants.ORDER_COMMANDS_TOPIC, command)
                    .chain(() -> emitAudit(orderId, SagaState.COMPLETED));
            });
    }

    // --- Compensación ----------------------------------------------------

    private Uni<Void> onStockReservationFailed(UUID orderId, String reason) {
        return sagaInstanceRepository.updateState(orderId, SagaState.FAILED, reason, SagaState.STARTED)
            .onItem().transformToUni(saga -> {
                if (saga == null) {
                    return Uni.createFrom().voidItem();
                }
                OrderCommandMessage cancelOrder = new OrderCommandMessage(UUID.randomUUID(), orderId,
                    MessagingConstants.COMMAND_TYPE_CANCEL_ORDER, reason, Instant.now());
                return emit(orderId, MessagingConstants.COMMAND_TYPE_CANCEL_ORDER,
                    MessagingConstants.ORDER_COMMANDS_TOPIC, cancelOrder)
                    .chain(() -> emitAudit(orderId, SagaState.FAILED));
            });
    }

    /** Compensaciones independientes — no importa el orden entre las dos, ninguna depende de la otra. */
    private Uni<Void> onPaymentDeclined(UUID orderId, String reason) {
        return sagaInstanceRepository.updateState(orderId, SagaState.FAILED, reason, SagaState.STOCK_RESERVED)
            .onItem().transformToUni(saga -> {
                if (saga == null) {
                    return Uni.createFrom().voidItem();
                }
                return emit(orderId, MessagingConstants.COMMAND_TYPE_RELEASE_STOCK,
                    MessagingConstants.INVENTORY_COMMANDS_TOPIC, releaseStockCommand(orderId, saga))
                    .chain(() -> emit(orderId, MessagingConstants.COMMAND_TYPE_CANCEL_ORDER,
                        MessagingConstants.ORDER_COMMANDS_TOPIC,
                        new OrderCommandMessage(UUID.randomUUID(), orderId,
                            MessagingConstants.COMMAND_TYPE_CANCEL_ORDER, reason, Instant.now())))
                    .chain(() -> emitAudit(orderId, SagaState.FAILED));
            });
    }

    /** Tres compensaciones independientes entre sí. */
    private Uni<Void> onShipmentFailed(UUID orderId, String reason) {
        return sagaInstanceRepository.updateState(orderId, SagaState.FAILED, reason, SagaState.PAYMENT_APPROVED)
            .onItem().transformToUni(saga -> {
                if (saga == null) {
                    return Uni.createFrom().voidItem();
                }
                PaymentCommandMessage refund = new PaymentCommandMessage(UUID.randomUUID(), orderId,
                    MessagingConstants.COMMAND_TYPE_REFUND_PAYMENT, saga.getTotalAmount(), saga.getCurrency(),
                    Instant.now());
                return emit(orderId, MessagingConstants.COMMAND_TYPE_REFUND_PAYMENT,
                    MessagingConstants.PAYMENT_COMMANDS_TOPIC, refund)
                    .chain(() -> emit(orderId, MessagingConstants.COMMAND_TYPE_RELEASE_STOCK,
                        MessagingConstants.INVENTORY_COMMANDS_TOPIC, releaseStockCommand(orderId, saga)))
                    .chain(() -> emit(orderId, MessagingConstants.COMMAND_TYPE_CANCEL_ORDER,
                        MessagingConstants.ORDER_COMMANDS_TOPIC,
                        new OrderCommandMessage(UUID.randomUUID(), orderId,
                            MessagingConstants.COMMAND_TYPE_CANCEL_ORDER, reason, Instant.now())))
                    .chain(() -> emitAudit(orderId, SagaState.FAILED));
            });
    }

    // --- Ayudantes compartidos ----------------------------------------------

    private InventoryCommandMessage releaseStockCommand(UUID orderId, SagaInstance saga) {
        List<InventoryItemPayload> items = readItemsSnapshot(saga.getItemsSnapshot()).stream()
            .map(item -> new InventoryItemPayload(item.productId(), item.quantity()))
            .collect(Collectors.toList());
        return new InventoryCommandMessage(UUID.randomUUID(), orderId,
            MessagingConstants.COMMAND_TYPE_RELEASE_STOCK, items, Instant.now());
    }

    private Uni<Void> ifNotProcessed(UUID eventId, java.util.function.Supplier<Uni<Void>> action) {
        return processedMessageRepository.alreadyProcessed(eventId)
            .onItem().transformToUni(processed -> Boolean.TRUE.equals(processed)
                ? Uni.createFrom().voidItem()
                : action.get());
    }

    private Uni<Void> logReceived(UUID sagaId, String stepName) {
        return sagaStepRepository.persist(SagaStep.received(sagaId, stepName)).replaceWithVoid();
    }

    private Uni<Void> emit(UUID sagaId, String stepName, String topic, Object message) {
        String json = writeValueAsString(message);
        return outboxRepository.persist(OutboxEventEntity.of(sagaId, stepName, topic, json))
            .chain(() -> sagaStepRepository.persist(SagaStep.sent(sagaId, stepName, topic, json)))
            .replaceWithVoid();
    }

    private Uni<Void> emitAudit(UUID sagaId, SagaState state) {
        SagaAuditMessage message = new SagaAuditMessage(UUID.randomUUID(), sagaId, state.name(),
            "saga-" + state.name().toLowerCase(), Instant.now());
        return outboxRepository.persist(OutboxEventEntity.of(sagaId, "SagaAudit",
                MessagingConstants.SAGA_EVENTS_TOPIC, writeValueAsString(message)))
            .replaceWithVoid();
    }

    private Uni<Void> markProcessed(UUID eventId) {
        return processedMessageRepository.persist(ProcessedMessageEntity.of(eventId)).replaceWithVoid();
    }

    private AddressPayload toAddressPayload(Address address) {
        return new AddressPayload(address.street(), address.city(), address.state(), address.zip(),
            address.country());
    }

    private List<OrderItemPayload> readItemsSnapshot(String itemsSnapshot) {
        try {
            return objectMapper.readValue(itemsSnapshot,
                objectMapper.getTypeFactory().constructCollectionType(List.class, OrderItemPayload.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize items snapshot", e);
        }
    }

    private String writeValueAsString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }
}
