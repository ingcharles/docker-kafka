package com.facturacion.comprobantes.application.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Mensaje consumido desde {@code facturacion.emisor.eventos.v1}, publicado
 * por firma-electronica-service. Mismo shape que el productor — ver la
 * nota en ARQUITECTURA.md sobre por qué esto es la única excepción a "cero
 * REST entre servicios" (vista materializada por eventos en vez de una
 * llamada síncrona).
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
