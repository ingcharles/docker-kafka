package com.facturacion.notificacion.application.dto;

/** Una variable que se puede usar en una plantilla: {@code marcador} es lo que se escribe, p. ej. {@code {{numero}}}. */
public record VariablePlantillaResponse(String nombre, String marcador, String descripcion) {
}
