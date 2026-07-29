package com.facturacion.sri.infrastructure.security;

import java.util.UUID;

import jakarta.enterprise.context.RequestScoped;

/** Poblado por {@link ApiKeyAuthFilter} — datos del api-key ya verificado para este request. */
@RequestScoped
public class TokenPrincipal {

    private UUID usuarioId;
    private String ruc;
    private String rol;
    private String establecimiento;
    private String puntoEmision;

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

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    /** {@code null} = api-key sin restricción de establecimiento (comportamiento por defecto). */
    public String getEstablecimiento() {
        return establecimiento;
    }

    public void setEstablecimiento(String establecimiento) {
        this.establecimiento = establecimiento;
    }

    /** {@code null} = api-key sin restricción de punto de emisión. */
    public String getPuntoEmision() {
        return puntoEmision;
    }

    public void setPuntoEmision(String puntoEmision) {
        this.puntoEmision = puntoEmision;
    }
}
