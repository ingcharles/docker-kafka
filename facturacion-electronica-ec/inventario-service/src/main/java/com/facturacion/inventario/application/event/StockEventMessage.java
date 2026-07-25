package com.facturacion.inventario.application.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Mensaje transportado por el tópico {@code facturacion.inventario.eventos.v1}.
 * {@code eventType} distingue StockReservado/StockNoDisponible (respuesta a
 * RESERVAR), StockConfirmado (respuesta a CONFIRMAR) y StockLiberado
 * (respuesta a LIBERAR). {@code itemsInsuficientes} solo se completa en
 * StockNoDisponible.
 */
public record StockEventMessage(
    UUID eventId,
    UUID comprobanteId,
    String eventType,
    List<UUID> itemsInsuficientes,
    Instant occurredAt) {
}
