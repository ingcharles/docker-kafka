package com.ecommerce.payment.application.event;

import java.time.Instant;
import java.util.UUID;

/** Mensaje transportado por el tópico {@code ecommerce.payment.events.v1} (PaymentApproved / PaymentDeclined / PaymentRefunded). */
public record PaymentEventMessage(
    UUID eventId,
    UUID orderId,
    String eventType,
    String reason,
    String gatewayReference,
    Instant occurredAt) {
}
