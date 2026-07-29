package com.facturacion.sri.application.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Mensaje consumido desde
 * {@code facturacion.empresa.configuracion.eventos.v1} — mantiene al día la
 * vista materializada {@code configuracion_empresa_local}. Copia
 * independiente de la clase homónima de firma-electronica-service (sin
 * librerías compartidas entre microservicios, mismo criterio que el resto
 * del stack).
 */
public record ConfiguracionEmpresaEventMessage(
    UUID eventId,
    String tipoEvento,
    String ruc,
    String clave,
    String valor,
    Instant occurredAt) {
}
