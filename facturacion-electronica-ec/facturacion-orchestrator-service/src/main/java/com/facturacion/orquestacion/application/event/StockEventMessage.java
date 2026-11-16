package com.facturacion.orquestacion.application.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Respuesta de inventario-service ({@code facturacion.inventario.eventos.v1}): StockReservado/StockNoDisponible
 * (a RESERVAR), StockConfirmado (a CONFIRMAR) y StockLiberado (a LIBERAR). {@code itemsInsuficientes} solo viene en
 * StockNoDisponible.
 */
public record StockEventMessage(
    UUID eventId,
    UUID comprobanteId,
    String eventType,
    List<UUID> itemsInsuficientes,
    Instant occurredAt) {
}
