package com.facturacion.identidad.domain.model.entity;

import java.time.Instant;
import java.util.UUID;

import com.facturacion.identidad.domain.model.enums.Rol;

/** Asocia un usuario a una empresa (ruc) con un rol — ver V1__init.sql para las limitaciones del flujo de asociación. */
public class UsuarioEmpresaRol {

    private UUID id;
    private UUID usuarioId;
    private String ruc;
    private Rol rol;
    private Instant creadoEn;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(UUID usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(Instant creadoEn) {
        this.creadoEn = creadoEn;
    }
}
