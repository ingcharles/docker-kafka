package com.facturacion.identidad.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CrearUsuarioRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    // bcrypt solo usa los primeros 72 bytes: más largo no daría más seguridad y se truncaría en silencio.
    @Size(min = 8, max = 72, message = "la contraseña debe tener entre 8 y 72 caracteres")
    private String password;

    @NotBlank
    private String nombres;

    @NotBlank
    private String apellidos;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

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
