package com.facturacion.identidad.application.dto;

import jakarta.validation.constraints.NotBlank;

/** No incluye email a propósito — cambiar el email de login es un flujo más sensible (unicidad, posible reverificación) fuera de alcance de esta versión. */
public class ActualizarPerfilRequest {

    @NotBlank
    private String nombres;

    @NotBlank
    private String apellidos;

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }
}
