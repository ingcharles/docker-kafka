package com.ecommerce.shipping.application.event;

import java.time.Instant;
import java.util.UUID;

/** Mensaje transportado por el tópico {@code ecommerce.shipping.events.v1} (ShipmentCreated / ShipmentFailed / ShipmentCancelled). */
public record ShippingEventMessage(
    UUID eventId,
    UUID orderId,
    String eventType,
    String reason,
    String trackingNumber,
    Instant occurredAt) {
}
