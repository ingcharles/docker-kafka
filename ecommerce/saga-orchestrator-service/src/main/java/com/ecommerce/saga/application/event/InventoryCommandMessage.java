package com.ecommerce.saga.application.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Se produce hacia el tópico {@code ecommerce.inventory.commands.v1} (ReserveStock / ReleaseStock). */
public record InventoryCommandMessage(
    UUID eventId,
    UUID orderId,
    String commandType,
    List<InventoryItemPayload> items,
    Instant occurredAt) {
}
