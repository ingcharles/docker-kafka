package com.facturacion.notificacion.application.dto;

import com.facturacion.notificacion.domain.model.enums.VariableCorreo;

/** Una variable que se puede usar en una plantilla: {@code marcador} es lo que se escribe, p. ej. {@code {{numero}}}. */
public record VariablePlantillaResponse(String nombre, String marcador, String descripcion) {

    public static VariablePlantillaResponse desde(VariableCorreo variable) {
        return new VariablePlantillaResponse(variable.nombre(), variable.marcador(), variable.descripcion());
    }
}
