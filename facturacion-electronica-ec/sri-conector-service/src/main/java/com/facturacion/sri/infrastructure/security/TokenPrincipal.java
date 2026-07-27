package com.facturacion.sri.infrastructure.security;

import java.util.UUID;

import jakarta.enterprise.context.RequestScoped;

/** Poblado por {@link ApiKeyAuthFilter} — datos del api-key ya verificado para este request. */
@RequestScoped
public class TokenPrincipal {

    private UUID usuarioId;
    private String ruc;
    private String rol;

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
}
