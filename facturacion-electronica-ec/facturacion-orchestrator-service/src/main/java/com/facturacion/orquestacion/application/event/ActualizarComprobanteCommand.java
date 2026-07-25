package com.facturacion.orquestacion.application.event;

import java.time.Instant;
import java.util.UUID;

/** Mensaje publicado en {@code facturacion.comprobante.comandos.v1} — copia local del command que comprobantes-service consume. */
public record ActualizarComprobanteCommand(
    UUID eventId,
    UUID comprobanteId,
    String tipoComando,
    String xmlFirmado,
    String xmlAutorizado,
    String numeroAutorizacion,
    Instant fechaAutorizacion,
    String motivoRechazo,
    Instant occurredAt) {
}
