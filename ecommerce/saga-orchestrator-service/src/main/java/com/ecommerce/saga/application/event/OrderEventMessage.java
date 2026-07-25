package com.ecommerce.saga.application.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Se consume desde el tópico {@code ecommerce.order.events.v1} — es lo que arranca la saga. */
public record OrderEventMessage(
    UUID eventId,
    UUID orderId,
    String eventType,
    String customerId,
    BigDecimal totalAmount,
    String currency,
    List<OrderItemPayload> items,
    AddressPayload shippingAddress,
    Instant occurredAt) {
}
