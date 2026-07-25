package com.ecommerce.payment.application.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Mensaje transportado por el tópico {@code ecommerce.payment.commands.v1} (ChargePayment / RefundPayment). */
public record PaymentCommandMessage(
    UUID eventId,
    UUID orderId,
    String commandType,
    BigDecimal amount,
    String currency,
    Instant occurredAt) {
}
