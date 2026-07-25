package com.facturacion.comprobantes.application.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Mensaje consumido desde {@code facturacion.comprobante.comandos.v1},
 * enviado siempre por facturacion-orchestrator-service a medida que avanza
 * la saga. {@code tipoComando} decide qué campos son relevantes — ver
 * {@code MessagingConstants.COMANDO_*} y {@code EstadoComprobante} para el
 * detalle de qué se guarda en cada transición.
 */
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
