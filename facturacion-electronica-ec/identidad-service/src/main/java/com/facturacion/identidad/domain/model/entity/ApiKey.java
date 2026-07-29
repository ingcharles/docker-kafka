package com.facturacion.identidad.domain.model.entity;

import java.time.Instant;
import java.util.UUID;

import com.facturacion.identidad.domain.model.enums.Rol;

/**
 * Metadatos de una api-key emitida — el valor real (el JWT firmado) nunca
 * se guarda acá, solo su {@code jti}. Ver V1__init.sql para el criterio de
 * revocación de "mejor esfuerzo".
 */
public class ApiKey {

    private UUID id;
    private UUID usuarioId;
    private String nombre;
    private String ruc;
    private Rol rol;
    private String establecimiento;
    private String puntoEmision;
    private String jti;
    private Instant fechaCreacion;
    private Instant fechaExpiracion;
    private boolean revocada;
    private Instant revocadaEn;

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

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
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

    public String getEstablecimiento() {
        return establecimiento;
    }

    public void setEstablecimiento(String establecimiento) {
        this.establecimiento = establecimiento;
    }

    public String getPuntoEmision() {
        return puntoEmision;
    }

    public void setPuntoEmision(String puntoEmision) {
        this.puntoEmision = puntoEmision;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Instant getFechaExpiracion() {
        return fechaExpiracion;
    }

    public void setFechaExpiracion(Instant fechaExpiracion) {
        this.fechaExpiracion = fechaExpiracion;
    }

    public boolean isRevocada() {
        return revocada;
    }

    public void setRevocada(boolean revocada) {
        this.revocada = revocada;
    }

    public Instant getRevocadaEn() {
        return revocadaEn;
    }

    public void setRevocadaEn(Instant revocadaEn) {
        this.revocadaEn = revocadaEn;
    }
}
