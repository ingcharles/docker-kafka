package com.facturacion.firma.application.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Mensaje transportado por el tópico
 * {@code facturacion.empresa.establecimientos.eventos.v1}. Un solo tópico
 * para dos formas distintas (establecimiento y punto de emisión) — mismo
 * sub-agregado, mismo único consumidor ({@code comprobantes-service}),
 * discriminadas por {@code tipoEvento} ({@code EstablecimientoRegistrado},
 * {@code EstablecimientoActualizado}, {@code PuntoEmisionRegistrado},
 * {@code PuntoEmisionActualizado}) — mismo patrón multi-forma que
 * {@code ComprobanteEventMessage} en notificacion-service, no el de
 * {@code ItemEventMessage} (una sola forma). {@code establecimientoCodigo}
 * se reutiliza en ambas formas: es el código propio en un evento de
 * establecimiento, o el código del establecimiento padre en un evento de
 * punto de emisión — así el consumidor puede indexar {@code punto_emision_local}
 * por (ruc, establecimientoCodigo, codigo) sin depender de ningún UUID
 * interno de este servicio.
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
