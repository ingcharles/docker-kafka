package com.ecommerce.order.application.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Mensaje transportado por el tópico {@code ecommerce.order.events.v1}. Una
 * sola forma de record cubre todo el tópico (hoy solo existe el tipo
 * {@code OrderCreated}); el campo {@code eventType} es el discriminador que
 * indica de qué tipo de evento se trata. Se sigue esta misma convención en
 * todos los tópicos del sistema, en vez de crear una clase Java distinta por
 * cada tipo de mensaje.
 */
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
