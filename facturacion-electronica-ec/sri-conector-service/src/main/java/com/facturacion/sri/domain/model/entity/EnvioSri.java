package com.facturacion.sri.domain.model.entity;

import java.time.Instant;
import java.util.UUID;

import com.facturacion.sri.domain.model.enums.AmbienteSri;
import com.facturacion.sri.domain.model.enums.EstadoEnvioSri;

/**
 * Un intento de envío/autorización de un comprobante ante el SRI — el
 * agregado de este servicio. Deliberadamente no duplica el Comprobante de
 * comprobantes-service (sin comprador, detalle, totales): solo lo
 * necesario para hablarle al SRI y recordar en qué quedó.
 */
public class EnvioSri {

    private UUID id;
    private UUID comprobanteId;
    private String claveAcceso;
    private String empresaRuc;
    private AmbienteSri ambiente;
    private String xmlFirmado;

    private EstadoEnvioSri estado;
    private String mensajesRecepcion;

    private int intentosAutorizacion;
    private Instant ultimaConsultaEn;

    private String xmlAutorizado;
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

    public String getEmpresaRuc() {
        return empresaRuc;
    }

    public void setEmpresaRuc(String empresaRuc) {
        this.empresaRuc = empresaRuc;
    }

    public AmbienteSri getAmbiente() {
        return ambiente;
    }

    public void setAmbiente(AmbienteSri ambiente) {
        this.ambiente = ambiente;
    }

    public String getXmlFirmado() {
        return xmlFirmado;
    }

    public void setXmlFirmado(String xmlFirmado) {
        this.xmlFirmado = xmlFirmado;
    }

    public EstadoEnvioSri getEstado() {
        return estado;
    }

    public void setEstado(EstadoEnvioSri estado) {
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

    public String getXmlAutorizado() {
        return xmlAutorizado;
    }

    public void setXmlAutorizado(String xmlAutorizado) {
        this.xmlAutorizado = xmlAutorizado;
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
