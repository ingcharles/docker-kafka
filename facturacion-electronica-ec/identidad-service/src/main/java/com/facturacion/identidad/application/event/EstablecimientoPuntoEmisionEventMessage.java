package com.facturacion.identidad.application.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Mensaje consumido desde
 * {@code facturacion.empresa.establecimientos.eventos.v1} — mantiene al día
 * las vistas materializadas {@code establecimiento_local}/{@code punto_emision_local}.
 * Copia independiente de la clase homónima de firma-electronica-service (sin
 * librerías compartidas entre microservicios, mismo criterio que el resto
 * del stack). Un solo record para las dos formas (establecimiento y punto de
 * emisión), discriminadas por {@code tipoEvento}.
 */
public record EstablecimientoPuntoEmisionEventMessage(
    UUID eventId,
    String tipoEvento,
    String ruc,
    String establecimientoCodigo,
    String direccion,
    String tipoEstablecimiento,
    String puntoEmisionCodigo,
    String descripcionPuntoEmision,
    boolean activo,
    Instant occurredAt) {
}
