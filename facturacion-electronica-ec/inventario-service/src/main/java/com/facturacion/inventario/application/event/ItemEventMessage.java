package com.facturacion.inventario.application.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Mensaje transportado por {@code facturacion.inventario.item.eventos.v1} —
 * la vista de catálogo que otros servicios (hoy: comprobantes-service)
 * pueden materializar localmente para no depender de una llamada REST
 * síncrona. Lleva la clasificación tributaria completa del ítem para que
 * comprobantes-service pueda derivar el impuesto de una línea de detalle
 * automáticamente a partir del {@code itemId}, sin que el llamador tenga
 * que mandarlo a mano — ver {@code ComprobanteService.construirDetalle} en
 * comprobantes-service.
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
