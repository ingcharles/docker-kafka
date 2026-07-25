package com.facturacion.orquestacion.application.event;

import java.time.Instant;
import java.util.UUID;

/** Mensaje consumido desde {@code facturacion.firma.eventos.v1} — copia local del evento que firma-electronica-service publica. */
public record ComprobanteFirmadoEventMessage(
    UUID eventId,
    UUID comprobanteId,
    String eventType,
    String xmlFirmado,
    String motivoError,
    Instant occurredAt) {
}
