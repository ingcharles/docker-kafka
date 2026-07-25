package com.facturacion.notificacion.application.event;

import java.time.Instant;
import java.util.UUID;

/** Mensaje publicado en {@code facturacion.notificacion.eventos.v1} — el orquestador lo traduce a {@code MARCAR_NOTIFICADO} hacia comprobantes-service. */
public record NotificacionResultadoEventMessage(
    UUID eventId,
    UUID comprobanteId,
    String eventType,
    String destinatario,
    String motivoError,
    Instant occurredAt) {
}
