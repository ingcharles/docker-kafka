package com.facturacion.firma.application.dto;

import java.time.Instant;
import java.util.UUID;

public class ConfiguracionEmisorResponse {

    private UUID id;
    private UUID emisorId;
    private String clave;
    private String valor;
    private Instant creadoEn;
    private Instant actualizadoEn;

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
