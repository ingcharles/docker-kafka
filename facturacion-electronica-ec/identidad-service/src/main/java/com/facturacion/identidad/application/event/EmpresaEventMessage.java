package com.facturacion.identidad.application.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Mensaje consumido desde {@code facturacion.empresa.eventos.v1}, publicado
 * por firma-electronica-service — mismo shape que su {@code EmpresaEventMessage}
 * (copia independiente, sin librería compartida entre microservicios, mismo
 * criterio que el resto del stack). Este servicio solo usa {@code ruc} y
 * {@code activo}; el resto de campos se deserializan igual pero se ignoran
 * al mapear a {@link com.facturacion.identidad.domain.model.entity.EmpresaLocal}.
 */
public record EmpresaEventMessage(
    UUID eventId,
    UUID empresaId,
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
    /** Solo en {@code EmpresaRegistrada} — {@code null} en {@code EmpresaActualizada}. Ver {@code EmpresaEventConsumer}. */
    UUID usuarioCreadorId,
    Instant occurredAt) {
}
