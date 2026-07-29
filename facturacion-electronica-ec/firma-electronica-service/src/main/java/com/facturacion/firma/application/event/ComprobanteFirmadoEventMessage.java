package com.facturacion.firma.application.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Mensaje transportado por el tópico {@code facturacion.firma.eventos.v1}.
 * {@code eventType} distingue {@code ComprobanteFirmado} (éxito, con
 * {@code xmlFirmado} presente) de {@code FirmaFallida} (con
 * {@code motivoError} presente en su lugar — por ejemplo, certificado
 * expirado o inexistente para esa empresa).
 */
public record ComprobanteFirmadoEventMessage(
    UUID eventId,
    UUID comprobanteId,
    String eventType,
    String xmlFirmado,
    String motivoError,
    Instant occurredAt) {
}
