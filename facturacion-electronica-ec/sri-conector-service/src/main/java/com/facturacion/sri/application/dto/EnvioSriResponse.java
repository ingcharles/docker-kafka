package com.facturacion.sri.application.dto;

import java.time.Instant;
import java.util.UUID;

public class EnvioSriResponse {

    private UUID id;
    private UUID comprobanteId;
    private String claveAcceso;
    private String ambiente;
    private String estado;
    private String mensajesRecepcion;
    private int intentosAutorizacion;
    private Instant ultimaConsultaEn;
    private String numeroAutorizacion;
    private Instant fechaAutorizacion;
    private String motivoRechazo;
    private Instant creadoEn;
    private Instant actualizadoEn;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getComprobanteId() {
        return comprobanteId;
    }

    public void setComprobanteId(UUID comprobanteId) {
        this.comprobanteId = comprobanteId;
    }

    public String getClaveAcceso() {
        return claveAcceso;
    }

    public void setClaveAcceso(String claveAcceso) {
        this.claveAcceso = claveAcceso;
    }

    public String getAmbiente() {
        return ambiente;
    }

    public void setAmbiente(String ambiente) {
        this.ambiente = ambiente;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMensajesRecepcion() {
        return mensajesRecepcion;
    }

    public void setMensajesRecepcion(String mensajesRecepcion) {
        this.mensajesRecepcion = mensajesRecepcion;
    }

    public int getIntentosAutorizacion() {
        return intentosAutorizacion;
    }

    public void setIntentosAutorizacion(int intentosAutorizacion) {
        this.intentosAutorizacion = intentosAutorizacion;
    }

    public Instant getUltimaConsultaEn() {
        return ultimaConsultaEn;
    }

    public void setUltimaConsultaEn(Instant ultimaConsultaEn) {
        this.ultimaConsultaEn = ultimaConsultaEn;
    }

    public String getNumeroAutorizacion() {
        return numeroAutorizacion;
    }

    public void setNumeroAutorizacion(String numeroAutorizacion) {
        this.numeroAutorizacion = numeroAutorizacion;
    }

    public Instant getFechaAutorizacion() {
        return fechaAutorizacion;
    }

    public void setFechaAutorizacion(Instant fechaAutorizacion) {
        this.fechaAutorizacion = fechaAutorizacion;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
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
