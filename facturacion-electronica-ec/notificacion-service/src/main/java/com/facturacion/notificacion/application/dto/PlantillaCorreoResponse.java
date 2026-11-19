package com.facturacion.notificacion.application.dto;

import java.time.Instant;
import java.util.UUID;

import com.facturacion.notificacion.domain.model.entity.PlantillaCorreoEmpresa;

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

    public static PlantillaCorreoResponse desde(PlantillaCorreoEmpresa plantilla) {
        return new PlantillaCorreoResponse(plantilla.getId(), plantilla.getTipoComprobante(), plantilla.getAsunto(),
            plantilla.getCuerpoHtml(), plantilla.getCuerpoTexto(), plantilla.getVersion(), plantilla.isActiva(),
            plantilla.getCreadoEn(), plantilla.getActualizadoEn());
    }
}
