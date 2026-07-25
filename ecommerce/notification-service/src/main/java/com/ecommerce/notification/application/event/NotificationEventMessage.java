package com.ecommerce.notification.application.event;

import java.time.Instant;
import java.util.UUID;

/** Mensaje transportado por el tópico {@code ecommerce.notification.events.v1} (NotificationSent / NotificationFailed). */
public record NotificationEventMessage(
    UUID eventId,
    UUID orderId,
    String eventType,
    String reason,
    Instant occurredAt) {
}
