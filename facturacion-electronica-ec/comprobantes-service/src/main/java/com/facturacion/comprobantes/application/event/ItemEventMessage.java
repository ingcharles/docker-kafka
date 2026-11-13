package com.facturacion.comprobantes.application.event;

import java.math.BigDecimal;
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
    String codigoImpuesto,
    String codigoPorcentaje,
    boolean tieneIce,
    String codigoPorcentajeIce,
    BigDecimal tarifaIce,
    boolean activo,
    Instant occurredAt) {
}
