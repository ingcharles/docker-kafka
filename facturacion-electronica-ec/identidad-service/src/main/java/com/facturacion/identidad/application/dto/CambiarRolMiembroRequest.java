package com.facturacion.identidad.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CambiarRolMiembroRequest {

    @NotBlank
    @Pattern(regexp = "ADMIN|OPERADOR|LECTURA", message = "el rol debe ser ADMIN, OPERADOR o LECTURA")
    private String rol;

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }
}
