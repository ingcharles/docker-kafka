package com.ecommerce.notification.application.event;

import java.time.Instant;
import java.util.UUID;

/** Mensaje transportado por el tópico {@code ecommerce.notification.commands.v1} (SendNotification). */
public record NotificationCommandMessage(
    UUID eventId,
    UUID orderId,
    String commandType,
    String notificationType,
    String recipient,
    Instant occurredAt) {
}
