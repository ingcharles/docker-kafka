package com.facturacion.firma.domain.model.entity;

import java.time.Instant;
import java.util.UUID;

/**
 * Par clave/valor de configuración opcional de un emisor — ver la migración
 * V3 para el razonamiento de por qué esto es una tabla genérica separada de
 * las columnas estructurales de {@link Emisor}.
 */
public class ConfiguracionEmisor {

    private UUID id;
    private UUID emisorId;
    private String clave;
    private String valor;
    private Instant creadoEn;
    private Instant actualizadoEn;

    public ConfiguracionEmisor() {
    }

    public ConfiguracionEmisor(UUID id, UUID emisorId, String clave, String valor, Instant creadoEn,
            Instant actualizadoEn) {
        this.id = id;
        this.emisorId = emisorId;
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

    public UUID getEmisorId() {
        return emisorId;
    }

    public void setEmisorId(UUID emisorId) {
        this.emisorId = emisorId;
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
