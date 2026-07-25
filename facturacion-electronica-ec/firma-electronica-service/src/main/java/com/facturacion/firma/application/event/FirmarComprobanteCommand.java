package com.facturacion.firma.application.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Mensaje transportado por el tópico {@code facturacion.firma.comandos.v1},
 * enviado por {@code facturacion-orchestrator-service} una vez que
 * {@code comprobantes-service} generó el XML sin firmar de un comprobante.
 */
public record FirmarComprobanteCommand(
    UUID eventId,
    UUID comprobanteId,
    UUID emisorId,
    String xmlSinFirmar,
    Instant occurredAt) {
}
