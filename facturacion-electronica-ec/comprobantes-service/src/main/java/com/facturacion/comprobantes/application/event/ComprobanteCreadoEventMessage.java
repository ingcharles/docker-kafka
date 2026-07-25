package com.facturacion.comprobantes.application.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Mensaje publicado en {@code facturacion.comprobante.eventos.v1} al crear
 * un comprobante. Lleva {@code xmlGenerado} embebido para que
 * facturacion-orchestrator-service lo reenvíe tal cual dentro del comando
 * {@code FirmarComprobante} hacia firma-electronica-service — este servicio
 * nunca llama a firma-electronica-service directamente.
 * <p>
 * {@code eventType} es obligatorio (no solo un detalle de bookkeeping del
 * outbox): el mismo tópico también lleva {@code ComprobanteAutorizadoEventMessage},
 * con una forma distinta — cualquier consumidor necesita este campo para
 * distinguir uno de otro (ver notificacion-service, que ignora todo lo que
 * no sea ComprobanteAutorizado).
 */
public record ComprobanteCreadoEventMessage(
    UUID eventId,
    UUID comprobanteId,
    String eventType,
    String emisorRuc,
    String claveAcceso,
    String ambiente,
    String xmlGenerado,
    Instant occurredAt) {
}
