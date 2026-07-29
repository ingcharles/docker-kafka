package com.facturacion.firma.domain.model.entity;

import java.time.Instant;
import java.util.UUID;

/**
 * Punto de emisión (código de 3 dígitos, {@code ptoEmi} en la ficha técnica
 * del SRI) de un {@link Establecimiento}. Todo establecimiento nace con uno
 * por defecto — ver {@link Establecimiento}.
 */
public class PuntoEmision {

    private UUID id;
    private UUID establecimientoId;
    private String codigo;
    private String descripcion;
    private boolean activo;
    private Instant creadoEn;
    private Instant actualizadoEn;

    public PuntoEmision() {
    }

    public PuntoEmision(UUID id, UUID establecimientoId, String codigo, String descripcion, boolean activo,
            Instant creadoEn, Instant actualizadoEn) {
        this.id = id;
        this.establecimientoId = establecimientoId;
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.activo = activo;
        this.creadoEn = creadoEn;
        this.actualizadoEn = actualizadoEn;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getEstablecimientoId() {
        return establecimientoId;
    }

    public void setEstablecimientoId(UUID establecimientoId) {
        this.establecimientoId = establecimientoId;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(Instant creadoEn) {
        this.creadoEn = creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    public void setActualizadoEn(Instant actualizadoEn) {
        this.actualizadoEn = actualizadoEn;
    }
}
