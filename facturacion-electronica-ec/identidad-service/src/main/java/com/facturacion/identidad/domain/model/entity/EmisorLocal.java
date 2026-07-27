package com.facturacion.identidad.domain.model.entity;

import java.time.Instant;
import java.util.UUID;

/**
 * Copia local de solo lectura de un Emisor de firma-electronica-service —
 * solo lo mínimo que este servicio necesita: confirmar que un ruc
 * corresponde a un Emisor real y activo antes de dejar que un usuario se
 * asocie a él (ver {@code UsuarioService.asociarEmpresa}). Se actualiza
 * consumiendo {@code facturacion.emisor.eventos.v1}; nunca se escribe desde
 * este servicio por ningún otro camino.
 */
public class EmisorLocal {

    private UUID id;
    private String ruc;
    private String razonSocial;
    private boolean activo;
    private Instant actualizadoEn;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    public void setActualizadoEn(Instant actualizadoEn) {
        this.actualizadoEn = actualizadoEn;
    }
}
