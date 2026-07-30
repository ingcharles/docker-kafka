package com.facturacion.identidad.application.dto;

import jakarta.validation.constraints.NotBlank;

public class CambiarRolMiembroRequest {

    @NotBlank
    private String rol;

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }
}
