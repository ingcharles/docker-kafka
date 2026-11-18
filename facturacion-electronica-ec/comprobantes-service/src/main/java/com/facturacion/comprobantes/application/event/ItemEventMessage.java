package com.facturacion.comprobantes.application.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Mensaje consumido desde {@code facturacion.inventario.item.eventos.v1} —
 * mantiene al día la vista materializada {@code item_local}. Copia
 * independiente de la clase homónima de inventario-service (sin librerías
 * compartidas entre microservicios, mismo criterio que el resto del stack).
 */
public record ItemEventMessage(
    UUID eventId,
    UUID itemId,
    String eventType,
    String ruc,
    String tipo,
    String codigoPrincipal,
    String codigoAuxiliar,
    String descripcion,
    String unidadMedida,
    String codigoPorcentajeIva,
    String codigoIce,
    String codigoIrbpnr,
    boolean activo,
    Instant occurredAt) {
}
