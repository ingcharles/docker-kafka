package com.ecommerce.inventory.application.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Mensaje transportado por el tópico {@code ecommerce.inventory.commands.v1} (ReserveStock / ReleaseStock). */
public record InventoryCommandMessage(
    UUID eventId,
    UUID orderId,
    String commandType,
    List<InventoryItemPayload> items,
    Instant occurredAt) {
}
