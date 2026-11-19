package com.facturacion.notificacion.application.dto;

import java.time.Instant;

import com.facturacion.notificacion.domain.model.vo.VersionPlantillaCorreo;

/** Una versión anterior de la plantilla; {@code accion} dice por qué quedó en el historial (REEMPLAZADA o ELIMINADA). */
public record VersionPlantillaResponse(
    int version,
    String asunto,
    String cuerpoHtml,
    String cuerpoTexto,
    boolean activa,
    String accion,
    Instant registradoEn) {

    public static VersionPlantillaResponse desde(VersionPlantillaCorreo version) {
        return new VersionPlantillaResponse(version.version(), version.asunto(), version.cuerpoHtml(),
            version.cuerpoTexto(), version.activa(), version.accion().name(), version.registradoEn());
    }
}
