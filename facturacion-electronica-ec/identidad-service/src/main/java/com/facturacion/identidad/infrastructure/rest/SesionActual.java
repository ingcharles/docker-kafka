package com.facturacion.identidad.infrastructure.rest;

import java.util.UUID;

import jakarta.enterprise.context.RequestScoped;

/** Poblado por {@link SesionAuthFilter} — el usuarioId del token de sesión validado para este request. */
@RequestScoped
public class SesionActual {

    private UUID usuarioId;

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(UUID usuarioId) {
        this.usuarioId = usuarioId;
    }
}
