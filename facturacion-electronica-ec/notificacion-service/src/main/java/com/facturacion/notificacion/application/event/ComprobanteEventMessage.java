package com.facturacion.notificacion.application.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Mensaje consumido desde {@code facturacion.comprobante.eventos.v1} — ese
 * tópico lleva más de un tipo de evento (hoy: {@code ComprobanteCreado} y
 * {@code ComprobanteAutorizado}); este servicio solo actúa sobre el
 * segundo, así que este record solo modela los campos de ese tipo y deja
 * que Jackson ignore los campos que no reconoce y complete con null los que
 * no vengan (ver {@code ComprobanteEventMessageDeserializer}, que configura
 * el {@code ObjectMapper} para tolerar ambas cosas) — así no hace falta
 * mantener dos deserializadores ni filtrar antes de deserializar.
 */
public record ComprobanteEventMessage(
    UUID eventId,
    UUID comprobanteId,
    String claveAcceso,
    String eventType,
    String tipoComprobante,
    String compradorCorreo,
    String compradorRazonSocial,
    String ridePdfBase64,
    String xmlAutorizado,
    Instant occurredAt) {
}
