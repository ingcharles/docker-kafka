package com.facturacion.orquestacion.application.event;

import java.time.Instant;
import java.util.UUID;

/** Mensaje consumido desde {@code facturacion.sri.eventos.v1} — copia local del evento que sri-conector-service publica. */
public record SriResultadoEventMessage(
    UUID eventId,
    UUID comprobanteId,
    String claveAcceso,
    String eventType,
    String xmlAutorizado,
    String numeroAutorizacion,
    Instant fechaAutorizacion,
    String motivoRechazo,
    Instant occurredAt) {
}
