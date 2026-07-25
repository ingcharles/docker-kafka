package com.ecommerce.saga.application.event;

import java.time.Instant;
import java.util.UUID;

/** Se consume desde el tópico {@code ecommerce.notification.events.v1} (NotificationSent / NotificationFailed). */
public record NotificationEventMessage(
    UUID eventId,
    UUID orderId,
    String eventType,
    String reason,
    Instant occurredAt) {
}
