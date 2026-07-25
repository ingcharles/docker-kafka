package com.ecommerce.saga.application.event;

import java.time.Instant;
import java.util.UUID;

/** Se produce hacia el tópico {@code ecommerce.order.commands.v1} (ConfirmOrder / CancelOrder). */
public record OrderCommandMessage(
    UUID eventId,
    UUID orderId,
    String commandType,
    String reason,
    Instant occurredAt) {
}
