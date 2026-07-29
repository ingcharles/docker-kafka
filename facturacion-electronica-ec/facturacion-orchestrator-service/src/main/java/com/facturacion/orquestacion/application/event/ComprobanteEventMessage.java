package com.facturacion.orquestacion.application.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Mensaje consumido desde {@code facturacion.comprobante.eventos.v1} — ese
 * tópico lleva más de un tipo de evento ({@code ComprobanteCreado} y
 * {@code ComprobanteAutorizado}); este orquestador solo actúa sobre el
 * primero (el segundo lo consume directo notificacion-service, sin pasar
 * por acá). Solo modela los campos de ComprobanteCreado y deja que Jackson
 * ignore/complete con null el resto — ver
 * {@code ComprobanteEventMessageDeserializer}.
 */
public record ComprobanteEventMessage(
    UUID eventId,
    UUID comprobanteId,
    String eventType,
    String empresaRuc,
    String claveAcceso,
    String ambiente,
    String xmlGenerado,
    Instant occurredAt) {
}
