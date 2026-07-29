package com.facturacion.firma.application.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Mensaje transportado por el tópico
 * {@code facturacion.empresa.configuracion.eventos.v1} — un ajuste
 * clave/valor de {@code configuracion_empresa} cambió (o se eliminó, en cuyo
 * caso {@code valor} viaja en {@code null} y el consumidor debe revertir al
 * comportamiento por defecto). Genérico a propósito: viaja cualquier
 * {@code clave}, y cada consumidor decide cuáles le importan (hoy solo
 * {@code ride.color} tiene un consumidor real, en comprobantes-service).
 */
public record ConfiguracionEmpresaEventMessage(
    UUID eventId,
    String tipoEvento,
    String ruc,
    String clave,
    String valor,
    Instant occurredAt) {
}
