package com.facturacion.sri.application.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Mensaje publicado en {@code facturacion.sri.eventos.v1}. {@code eventType}
 * (ver {@code MessagingConstants.EVENT_TYPE_*}) distingue si es solo un aviso
 * de que Recepción aceptó el sobre (SriRecibido, informativo — comprobantes-service
 * no tiene un estado intermedio para esto) o el resultado final de
 * Autorización (SriAutorizado/SriNoAutorizado/SriRechazadoRecepcion), que es
 * lo que facturacion-orchestrator-service traduce a los comandos
 * MARCAR_AUTORIZADO/MARCAR_NO_AUTORIZADO hacia comprobantes-service.
 * {@code xmlAutorizado} lleva el bloque {@code <autorizacion>} completo
 * devuelto por el SRI, no una copia del XML firmado — ver EstadoComprobante
 * en comprobantes-service.
 */
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
