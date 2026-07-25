package com.facturacion.inventario.application.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Mensaje transportado por el tópico {@code facturacion.inventario.comandos.v1}.
 * {@code tipoComando} distingue RESERVAR (con {@code items} — al crear un
 * comprobante), CONFIRMAR (al autorizarse en el SRI, escribe el kardex) y
 * LIBERAR (al rechazarse/cancelarse, sin efecto contable). Enviado siempre
 * por {@code facturacion-orchestrator-service}.
 */
public record StockCommandMessage(
    UUID eventId,
    String tipoComando,
    UUID comprobanteId,
    List<ItemCantidadPayload> items,
    Instant occurredAt) {
}
