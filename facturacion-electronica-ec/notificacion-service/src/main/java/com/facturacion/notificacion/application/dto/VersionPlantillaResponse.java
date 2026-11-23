package com.facturacion.notificacion.application.dto;

import java.time.Instant;

/** Una versión anterior de la plantilla; {@code accion} dice por qué quedó en el historial (REEMPLAZADA o ELIMINADA). */
public record VersionPlantillaResponse(
    int version,
    String asunto,
    String cuerpoHtml,
    String cuerpoTexto,
    boolean activa,
    String accion,
    Instant registradoEn) {
}
