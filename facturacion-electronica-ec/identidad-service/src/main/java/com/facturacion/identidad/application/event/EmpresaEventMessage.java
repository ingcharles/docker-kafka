package com.facturacion.identidad.application.event;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Mensaje consumido desde {@code facturacion.empresa.eventos.v1}, publicado
 * por firma-electronica-service — mismo shape que su {@code EmpresaEventMessage}
 * (copia independiente, sin librería compartida entre microservicios, mismo
 * criterio que el resto del stack). Este servicio solo usa {@code ruc} y
 * {@code activo}; el resto de campos se deserializan igual pero se ignoran
 * al mapear a {@link com.facturacion.identidad.domain.model.entity.EmpresaLocal}. Ignora los campos que no conoce: un
 * campo que el productor quita o agrega no rompe la lectura de los eventos que ya están en Kafka.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
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
    String ambiente,
    String establecimientoDefecto,
    String puntoEmisionDefecto,
    boolean activo,
    /** Solo en {@code EmpresaRegistrada} — {@code null} en {@code EmpresaActualizada}. Ver {@code EmpresaEventConsumer}. */
    UUID usuarioCreadorId,
    Instant occurredAt) {
}
