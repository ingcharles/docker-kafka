package com.ecommerce.saga.application.event;

import java.time.Instant;
import java.util.UUID;

/** Se produce hacia el tópico {@code ecommerce.shipping.commands.v1} (CreateShipment / CancelShipment). */
public record ShippingCommandMessage(
    UUID eventId,
    UUID orderId,
    String commandType,
    AddressPayload shippingAddress,
    Instant occurredAt) {
}
