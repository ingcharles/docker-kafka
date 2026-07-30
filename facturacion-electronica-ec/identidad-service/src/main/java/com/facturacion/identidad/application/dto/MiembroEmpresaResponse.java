package com.facturacion.identidad.application.dto;

import java.time.Instant;
import java.util.UUID;

/** Un usuario con acceso a una empresa (ruc) — ver EmpresaMiembroResource. */
public class MiembroEmpresaResponse {

    private UUID usuarioId;
    private String email;
    private String nombres;
    private String apellidos;
    private String rol;
    private Instant creadoEn;

    public MiembroEmpresaResponse() {
    }

    public MiembroEmpresaResponse(UUID usuarioId, String email, String nombres, String apellidos, String rol,
            Instant creadoEn) {
        this.usuarioId = usuarioId;
        this.email = email;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.rol = rol;
        this.creadoEn = creadoEn;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(UUID usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(Instant creadoEn) {
        this.creadoEn = creadoEn;
    }
}
