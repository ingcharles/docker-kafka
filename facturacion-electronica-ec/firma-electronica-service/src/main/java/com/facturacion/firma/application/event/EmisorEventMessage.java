package com.facturacion.firma.application.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Mensaje transportado por el tópico {@code facturacion.emisor.eventos.v1}.
 * {@code tipoEvento} distingue {@code EmisorRegistrado} de {@code EmisorActualizado}.
 * Nunca lleva ningún dato del certificado — solo los campos que
 * {@code comprobantes-service} y {@code sri-conector-service} necesitan
 * mantener en su propia copia local de solo lectura (patrón de vista
 * materializada por eventos, la única excepción a "cero REST entre
 * servicios" en este stack: ver ARQUITECTURA.md).
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
