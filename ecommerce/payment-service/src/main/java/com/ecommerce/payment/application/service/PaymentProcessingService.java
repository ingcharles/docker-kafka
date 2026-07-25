package com.ecommerce.payment.application.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.ecommerce.payment.application.event.PaymentEventMessage;
import com.ecommerce.payment.domain.model.entity.Payment;
import com.ecommerce.payment.domain.model.enums.PaymentStatus;
import com.ecommerce.payment.domain.repository.PaymentRepository;
import com.ecommerce.payment.infrastructure.inbox.ProcessedMessageEntity;
import com.ecommerce.payment.infrastructure.inbox.ProcessedMessageRepository;
import com.ecommerce.payment.infrastructure.outbox.OutboxEventEntity;
import com.ecommerce.payment.infrastructure.outbox.OutboxEventRepository;
import com.ecommerce.payment.shared.constant.MessagingConstants;

/**
 * Pasarela de pago simulada: aquí no hay ninguna integración real con un
 * proveedor de pagos (PSP). Es un reemplazo completamente determinista,
 * hecho a propósito así para poder ejercitar el camino de compensación de
 * la saga sin depender de ningún servicio externo. La decisión de aprobar o
 * rechazar el cobro vive directamente en {@link #chargePayment} (variable
 * local {@code declined}); reemplázala por una llamada real a la pasarela
 * de pago el día que esto deje de ser una implementación de referencia/demo.
 */
@ApplicationScoped
public class PaymentProcessingService {

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxRepository;
    private final ProcessedMessageRepository processedMessageRepository;
    private final ObjectMapper objectMapper;
    private final BigDecimal declineThreshold;

    @Inject
    public PaymentProcessingService(PaymentRepository paymentRepository,
        OutboxEventRepository outboxRepository, ProcessedMessageRepository processedMessageRepository,
        ObjectMapper objectMapper,
        @ConfigProperty(name = "payment.simulation.decline-threshold", defaultValue = "5000")
        BigDecimal declineThreshold) {
        this.paymentRepository = paymentRepository;
        this.outboxRepository = outboxRepository;
        this.processedMessageRepository = processedMessageRepository;
        this.objectMapper = objectMapper;
        this.declineThreshold = declineThreshold;
    }

    @WithTransaction
    public Uni<Void> chargePayment(UUID eventId, UUID orderId, BigDecimal amount, String currency) {
        return processedMessageRepository.alreadyProcessed(eventId)
            .onItem().transformToUni(processed -> {
                if (Boolean.TRUE.equals(processed)) {
                    return Uni.createFrom().voidItem();
                }
                boolean declined = amount.compareTo(declineThreshold) > 0;
                Payment payment = Payment.createPending(orderId, amount, currency);
                String eventType;
                String reason = null;
                String gatewayReference = null;
                if (declined) {
                    payment.setStatus(PaymentStatus.DECLINED);
                    reason = "Amount " + amount + " exceeds decline threshold " + declineThreshold;
                    payment.setDeclineReason(reason);
                    eventType = MessagingConstants.EVENT_TYPE_PAYMENT_DECLINED;
                } else {
                    payment.setStatus(PaymentStatus.APPROVED);
                    gatewayReference = "SIM-" + UUID.randomUUID();
                    payment.setGatewayReference(gatewayReference);
                    eventType = MessagingConstants.EVENT_TYPE_PAYMENT_APPROVED;
                }
                String finalReason = reason;
                String finalGatewayReference = gatewayReference;
                return paymentRepository.persist(payment)
                    .chain(() -> recordOutcome(eventId, orderId, eventType, finalReason, finalGatewayReference));
            });
    }

    @WithTransaction
    public Uni<Void> refundPayment(UUID eventId, UUID orderId) {
        return processedMessageRepository.alreadyProcessed(eventId)
            .onItem().transformToUni(processed -> {
                if (Boolean.TRUE.equals(processed)) {
                    return Uni.createFrom().voidItem();
                }
                return paymentRepository.updateStatus(orderId, PaymentStatus.REFUNDED, null, null,
                        PaymentStatus.APPROVED)
                    .chain(() -> recordOutcome(eventId, orderId,
                        MessagingConstants.EVENT_TYPE_PAYMENT_REFUNDED, null, null));
            });
    }

    private Uni<Void> recordOutcome(UUID eventId, UUID orderId, String eventType, String reason,
        String gatewayReference) {
        PaymentEventMessage message = new PaymentEventMessage(
            UUID.randomUUID(), orderId, eventType, reason, gatewayReference, Instant.now());
        OutboxEventEntity outboxEvent = OutboxEventEntity.of(orderId, eventType,
            MessagingConstants.PAYMENT_EVENTS_TOPIC, writeValueAsString(message));
        return processedMessageRepository.persist(ProcessedMessageEntity.of(eventId))
            .chain(() -> outboxRepository.persist(outboxEvent))
            .replaceWithVoid();
    }

    private String writeValueAsString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }
}
