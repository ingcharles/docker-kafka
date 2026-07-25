package com.ecommerce.notification.application.service;

import java.time.Instant;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.ecommerce.notification.application.event.NotificationEventMessage;
import com.ecommerce.notification.domain.model.entity.Notification;
import com.ecommerce.notification.domain.model.enums.NotificationStatus;
import com.ecommerce.notification.domain.repository.NotificationRepository;
import com.ecommerce.notification.infrastructure.inbox.ProcessedMessageEntity;
import com.ecommerce.notification.infrastructure.inbox.ProcessedMessageRepository;
import com.ecommerce.notification.infrastructure.outbox.OutboxEventEntity;
import com.ecommerce.notification.infrastructure.outbox.OutboxEventRepository;
import com.ecommerce.notification.shared.constant.MessagingConstants;

/**
 * Pasarela de notificación simulada (reemplazo de un proveedor real de
 * email/SMS): falla de forma determinista para el destinatario centinela
 * {@value MessagingConstants#FAILURE_SENTINEL_RECIPIENT}. Una integración
 * real envolvería la llamada HTTP saliente con {@code @Retry} para manejar
 * fallos *transitorios* — pero reintentar esta simulación determinista no
 * cambiaría el resultado en absoluto, así que aquí no se aplica. Según el
 * diseño de la saga, una notificación fallida nunca dispara ninguna
 * compensación: para este punto la orden ya está pagada y despachada, así
 * que simplemente se registra el fallo y se continúa (ver
 * saga-orchestrator-service).
 */
@ApplicationScoped
public class NotificationSendingService {

    private final NotificationRepository notificationRepository;
    private final OutboxEventRepository outboxRepository;
    private final ProcessedMessageRepository processedMessageRepository;
    private final ObjectMapper objectMapper;

    @Inject
    public NotificationSendingService(NotificationRepository notificationRepository,
        OutboxEventRepository outboxRepository, ProcessedMessageRepository processedMessageRepository,
        ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.outboxRepository = outboxRepository;
        this.processedMessageRepository = processedMessageRepository;
        this.objectMapper = objectMapper;
    }

    @WithTransaction
    public Uni<Void> sendNotification(UUID eventId, UUID orderId, String notificationType,
        String recipient) {
        return processedMessageRepository.alreadyProcessed(eventId)
            .onItem().transformToUni(processed -> {
                if (Boolean.TRUE.equals(processed)) {
                    return Uni.createFrom().voidItem();
                }
                boolean fails = MessagingConstants.FAILURE_SENTINEL_RECIPIENT.equalsIgnoreCase(recipient);
                Notification notification = Notification.createNew(orderId, notificationType, recipient);
                String eventType;
                String reason = null;
                if (fails) {
                    notification.setStatus(NotificationStatus.FAILED);
                    reason = "Simulated delivery failure for recipient " + recipient;
                    notification.setFailureReason(reason);
                    eventType = MessagingConstants.EVENT_TYPE_NOTIFICATION_FAILED;
                } else {
                    notification.setStatus(NotificationStatus.SENT);
                    eventType = MessagingConstants.EVENT_TYPE_NOTIFICATION_SENT;
                }
                String finalReason = reason;
                return notificationRepository.persist(notification)
                    .chain(() -> recordOutcome(eventId, orderId, eventType, finalReason));
            });
    }

    private Uni<Void> recordOutcome(UUID eventId, UUID orderId, String eventType, String reason) {
        NotificationEventMessage message = new NotificationEventMessage(
            UUID.randomUUID(), orderId, eventType, reason, Instant.now());
        OutboxEventEntity outboxEvent = OutboxEventEntity.of(orderId, eventType,
            MessagingConstants.NOTIFICATION_EVENTS_TOPIC, writeValueAsString(message));
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
