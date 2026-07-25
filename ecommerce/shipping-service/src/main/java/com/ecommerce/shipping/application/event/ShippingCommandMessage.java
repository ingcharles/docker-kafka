package com.ecommerce.shipping.application.event;

import java.time.Instant;
import java.util.UUID;

/** Mensaje transportado por el tópico {@code ecommerce.shipping.commands.v1} (CreateShipment / CancelShipment). */
public record ShippingCommandMessage(
    UUID eventId,
    UUID orderId,
    String commandType,
    AddressPayload shippingAddress,
    Instant occurredAt) {
}
