package com.ecommerce.order.application.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Mensaje transportado por el tópico {@code ecommerce.order.commands.v1},
 * enviado siempre por el orquestador de la saga. El campo
 * {@code commandType} distingue entre ConfirmOrder y CancelOrder; el campo
 * {@code reason} solo se completa cuando el comando es CancelOrder (para
 * dejar registrado el motivo de la cancelación).
 */
public record OrderCommandMessage(
    UUID eventId,
    UUID orderId,
    String commandType,
    String reason,
    Instant occurredAt) {
}
