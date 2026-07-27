package com.facturacion.identidad.application.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Mensaje consumido desde {@code facturacion.emisor.eventos.v1}, publicado
 * por firma-electronica-service — mismo shape que su {@code EmisorEventMessage}
 * (copia independiente, sin librería compartida entre microservicios, mismo
 * criterio que el resto del stack). Este servicio solo usa {@code ruc} y
 * {@code activo}; el resto de campos se deserializan igual pero se ignoran
 * al mapear a {@link com.facturacion.identidad.domain.model.entity.EmisorLocal}.
 */
public record EmisorEventMessage(
    UUID eventId,
    UUID emisorId,
    String tipoEvento,
    String ruc,
    String razonSocial,
    String nombreComercial,
    String direccionMatriz,
    boolean obligadoContabilidad,
    boolean contribuyenteEspecial,
    String numeroContribuyenteEspecial,
    boolean esGranContribuyente,
    String numeroResolucionGranContribuyente,
    boolean vendeCombustibles,
    String ambiente,
    String establecimientoDefecto,
    String puntoEmisionDefecto,
    boolean activo,
    Instant occurredAt) {
}
