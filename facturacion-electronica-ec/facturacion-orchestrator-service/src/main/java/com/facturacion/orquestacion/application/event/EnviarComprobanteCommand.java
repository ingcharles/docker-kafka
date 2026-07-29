package com.facturacion.orquestacion.application.event;

import java.time.Instant;
import java.util.UUID;

/** Mensaje publicado en {@code facturacion.sri.comandos.v1} — copia local del command que sri-conector-service consume. */
public record EnviarComprobanteCommand(
    UUID eventId,
    UUID comprobanteId,
    String claveAcceso,
    String empresaRuc,
    String ambiente,
    String xmlFirmado,
    Instant occurredAt) {
}
