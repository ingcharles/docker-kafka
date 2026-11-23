package com.facturacion.notificacion.application.dto;

import java.time.Instant;
import java.util.UUID;

/** La plantilla de correo de una empresa tal como quedó guardada (el HTML ya limpiado). */
public record PlantillaCorreoResponse(
    UUID id,
    String tipoComprobante,
    String asunto,
    String cuerpoHtml,
    String cuerpoTexto,
    int version,
    boolean activa,
    Instant creadoEn,
    Instant actualizadoEn) {
}
