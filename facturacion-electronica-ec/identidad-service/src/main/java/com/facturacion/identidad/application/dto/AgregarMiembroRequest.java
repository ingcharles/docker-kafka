package com.facturacion.identidad.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Un ADMIN del ruc da acceso a un usuario ya registrado (por su correo) con el rol indicado. */
public class AgregarMiembroRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Pattern(regexp = "ADMIN|OPERADOR|LECTURA", message = "el rol debe ser ADMIN, OPERADOR o LECTURA")
    private String rol;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }
}
