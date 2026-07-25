package com.ecommerce.saga.application.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Se produce hacia el tópico {@code ecommerce.saga.events.v1} — es solo un
 * rastro de auditoría/observabilidad, hoy no tiene ningún consumidor en
 * este sistema. El campo {@code orderId} cumple doble función: es también
 * el id de la saga.
 */
public record SagaAuditMessage(
    UUID eventId,
    UUID orderId,
    String state,
    String step,
    Instant occurredAt) {
}
