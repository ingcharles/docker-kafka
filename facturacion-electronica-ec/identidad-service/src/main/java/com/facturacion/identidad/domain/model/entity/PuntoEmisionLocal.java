package com.facturacion.identidad.domain.model.entity;

import java.time.Instant;
import java.util.UUID;

/**
 * Copia local de solo lectura de un punto de emisión — ver
 * {@link EstablecimientoLocal}. Indexada por la clave natural (ruc,
 * establecimientoCodigo, codigo).
 */
public class PuntoEmisionLocal {

    private UUID id;
    private String ruc;
    private String establecimientoCodigo;
    private String codigo;
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

    public String getEstablecimientoCodigo() {
        return establecimientoCodigo;
    }

    public void setEstablecimientoCodigo(String establecimientoCodigo) {
        this.establecimientoCodigo = establecimientoCodigo;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
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
