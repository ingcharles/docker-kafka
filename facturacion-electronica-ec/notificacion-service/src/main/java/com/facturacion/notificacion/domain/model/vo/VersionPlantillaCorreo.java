package com.facturacion.notificacion.domain.model.vo;

import java.time.Instant;

import com.facturacion.notificacion.domain.model.enums.AccionHistorialPlantilla;

/** Una versión anterior de una plantilla de correo, tal como estaba cuando se reemplazó o se eliminó. */
public record VersionPlantillaCorreo(int version, String asunto, String cuerpoHtml, String cuerpoTexto, boolean activa,
        AccionHistorialPlantilla accion, Instant registradoEn) {
}
