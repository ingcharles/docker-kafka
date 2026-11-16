package com.facturacion.orquestacion.application.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Comando hacia inventario-service ({@code facturacion.inventario.comandos.v1}): RESERVAR (con {@code items}),
 * CONFIRMAR (escribe el kardex) o LIBERAR (devuelve la reserva). Misma forma que el de inventario-service.
 */
public record StockCommandMessage(
    UUID eventId,
    String tipoComando,
    UUID comprobanteId,
    List<ItemCantidad> items,
    Instant occurredAt) {
}
