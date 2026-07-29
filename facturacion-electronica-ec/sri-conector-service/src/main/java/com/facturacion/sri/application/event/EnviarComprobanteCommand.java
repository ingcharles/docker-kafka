package com.facturacion.sri.application.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Mensaje consumido desde {@code facturacion.sri.comandos.v1}, enviado por
 * facturacion-orchestrator-service una vez que firma-electronica-service
 * confirmó la firma (equivalente a {@code FirmarComprobanteCommand} en
 * firma-electronica-service, pero para el paso de envío al SRI).
 */
public record EnviarComprobanteCommand(
    UUID eventId,
    UUID comprobanteId,
    String claveAcceso,
    String empresaRuc,
    String ambiente,
    String xmlFirmado,
    Instant occurredAt) {
}
