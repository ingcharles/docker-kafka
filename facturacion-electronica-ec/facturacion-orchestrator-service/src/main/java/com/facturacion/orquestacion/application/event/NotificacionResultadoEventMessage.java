package com.facturacion.orquestacion.application.event;

import java.time.Instant;
import java.util.UUID;

/** Mensaje consumido desde {@code facturacion.notificacion.eventos.v1} — copia local del evento que notificacion-service publica. */
public record NotificacionResultadoEventMessage(
    UUID eventId,
    UUID comprobanteId,
    String eventType,
    String destinatario,
    String motivoError,
    Instant occurredAt) {
}
