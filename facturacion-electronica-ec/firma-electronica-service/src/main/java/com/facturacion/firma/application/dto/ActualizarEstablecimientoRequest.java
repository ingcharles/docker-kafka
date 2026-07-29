package com.facturacion.firma.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ActualizarEstablecimientoRequest {

    @NotBlank
    private String direccion;

    @NotBlank
    @Pattern(regexp = "MATRIZ|SUCURSAL", message = "tipo debe ser MATRIZ o SUCURSAL")
    private String tipo;

    private boolean activo = true;

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
