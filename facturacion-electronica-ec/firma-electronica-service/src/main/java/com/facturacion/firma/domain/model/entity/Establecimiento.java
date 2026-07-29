package com.facturacion.firma.domain.model.entity;

import java.time.Instant;
import java.util.UUID;

/**
 * Establecimiento (código de 3 dígitos, {@code estab} en la ficha técnica
 * del SRI) de una {@link Empresa}. Toda empresa nace con uno por defecto
 * (tipo MATRIZ, código = {@code Empresa.establecimientoDefecto}) creado en
 * la misma transacción que {@code EmpresaService.crearEmpresa} — este CRUD
 * es para registrar sucursales adicionales o corregir la dirección.
 */
public class Establecimiento {

    private UUID id;
    private UUID empresaId;
    private String codigo;
    private String direccion;
    private String tipo;
    private boolean activo;
    private Instant creadoEn;
    private Instant actualizadoEn;

    public Establecimiento() {
    }

    public Establecimiento(UUID id, UUID empresaId, String codigo, String direccion, String tipo, boolean activo,
            Instant creadoEn, Instant actualizadoEn) {
        this.id = id;
        this.empresaId = empresaId;
        this.codigo = codigo;
        this.direccion = direccion;
        this.tipo = tipo;
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

    public UUID getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(UUID empresaId) {
        this.empresaId = empresaId;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
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
