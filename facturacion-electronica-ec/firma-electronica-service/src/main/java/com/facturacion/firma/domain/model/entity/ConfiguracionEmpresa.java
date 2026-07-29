package com.facturacion.firma.domain.model.entity;

import java.time.Instant;
import java.util.UUID;

/**
 * Par clave/valor de configuración opcional de una empresa — ver la migración
 * V3 para el razonamiento de por qué esto es una tabla genérica separada de
 * las columnas estructurales de {@link Empresa}.
 */
public class ConfiguracionEmpresa {

    private UUID id;
    private UUID empresaId;
    private String clave;
    private String valor;
    private Instant creadoEn;
    private Instant actualizadoEn;

    public ConfiguracionEmpresa() {
    }

    public ConfiguracionEmpresa(UUID id, UUID empresaId, String clave, String valor, Instant creadoEn,
            Instant actualizadoEn) {
        this.id = id;
        this.empresaId = empresaId;
        this.clave = clave;
        this.valor = valor;
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

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
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
