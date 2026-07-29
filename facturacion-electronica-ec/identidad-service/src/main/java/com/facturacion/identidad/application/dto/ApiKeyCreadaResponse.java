package com.facturacion.identidad.application.dto;

import java.time.Instant;
import java.util.UUID;

/** Única vez que el valor real de la api-key es visible — no se puede volver a consultar después. */
public class ApiKeyCreadaResponse {

    private UUID id;
    private String nombre;
    private String ruc;
    private String rol;
    private String establecimiento;
    private String puntoEmision;
    private String valor;
    private Instant fechaCreacion;
    private Instant fechaExpiracion;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
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

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
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
}
