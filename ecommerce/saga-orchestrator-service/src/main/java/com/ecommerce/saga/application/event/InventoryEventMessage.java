package com.ecommerce.saga.application.event;

import java.time.Instant;
import java.util.UUID;

/** Se consume desde el tópico {@code ecommerce.inventory.events.v1} (StockReserved / StockReservationFailed / StockReleased). */
public record InventoryEventMessage(
    UUID eventId,
    UUID orderId,
    String eventType,
    String reason,
    Instant occurredAt) {
}
