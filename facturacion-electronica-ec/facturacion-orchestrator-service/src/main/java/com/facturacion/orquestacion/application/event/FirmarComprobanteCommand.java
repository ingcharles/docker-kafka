package com.facturacion.orquestacion.application.event;

import java.time.Instant;
import java.util.UUID;

/** Mensaje publicado en {@code facturacion.firma.comandos.v1} — copia local del command que firma-electronica-service consume. */
public record FirmarComprobanteCommand(
    UUID eventId,
    UUID comprobanteId,
    UUID emisorId,
    String xmlSinFirmar,
    Instant occurredAt) {
}
