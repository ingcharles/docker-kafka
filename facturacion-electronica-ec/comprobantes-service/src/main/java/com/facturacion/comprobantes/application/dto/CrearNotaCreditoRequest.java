package com.facturacion.comprobantes.application.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Modifica (total o parcialmente) un comprobante ya emitido, con el mismo
 * shape de detalle/impuestos que Factura (Tabla 1 vs infoNotaCredito de la
 * ficha técnica) — solo cambia el encabezado, que además referencia al
 * documento que se está modificando.
 */
public class CrearNotaCreditoRequest {

    @NotNull
    @Valid
    private EmisorRequest emisor;

    @NotBlank
    private String compradorTipoIdentificacion;

    @NotBlank
    private String compradorIdentificacion;

    @NotBlank
    private String compradorRazonSocial;

    private String compradorDireccion;

    private String compradorCorreo;

    /** codDocModificado — Tabla 6 de la ficha técnica (01=Factura, etc.). */
    @NotBlank
    private String docModificadoTipo;

    @NotBlank
    private String docModificadoNumero;

    /** dd/mm/aaaa */
    @NotBlank
    private String docModificadoFecha;

    /** Obligatorio según el XSD real del SRI (motivo general de la nota de crédito, ej. "DEVOLUCION DE MERCADERIA"). */
    @NotBlank
    private String motivo;

    private String moneda = "DOLAR";

    @NotEmpty
    @Valid
    private List<DetalleComprobanteRequest> detalles;

    @Valid
    private List<CampoAdicionalRequest> campoAdicional = List.of();

    public EmisorRequest getEmisor() {
        return emisor;
    }

    public void setEmisor(EmisorRequest emisor) {
        this.emisor = emisor;
    }

    public String getCompradorTipoIdentificacion() {
        return compradorTipoIdentificacion;
    }

    public void setCompradorTipoIdentificacion(String compradorTipoIdentificacion) {
        this.compradorTipoIdentificacion = compradorTipoIdentificacion;
    }

    public String getCompradorIdentificacion() {
        return compradorIdentificacion;
    }

    public void setCompradorIdentificacion(String compradorIdentificacion) {
        this.compradorIdentificacion = compradorIdentificacion;
    }

    public String getCompradorRazonSocial() {
        return compradorRazonSocial;
    }

    public void setCompradorRazonSocial(String compradorRazonSocial) {
        this.compradorRazonSocial = compradorRazonSocial;
    }

    public String getCompradorDireccion() {
        return compradorDireccion;
    }

    public void setCompradorDireccion(String compradorDireccion) {
        this.compradorDireccion = compradorDireccion;
    }

    public String getCompradorCorreo() {
        return compradorCorreo;
    }

    public void setCompradorCorreo(String compradorCorreo) {
        this.compradorCorreo = compradorCorreo;
    }

    public String getDocModificadoTipo() {
        return docModificadoTipo;
    }

    public void setDocModificadoTipo(String docModificadoTipo) {
        this.docModificadoTipo = docModificadoTipo;
    }

    public String getDocModificadoNumero() {
        return docModificadoNumero;
    }

    public void setDocModificadoNumero(String docModificadoNumero) {
        this.docModificadoNumero = docModificadoNumero;
    }

    public String getDocModificadoFecha() {
        return docModificadoFecha;
    }

    public void setDocModificadoFecha(String docModificadoFecha) {
        this.docModificadoFecha = docModificadoFecha;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public List<DetalleComprobanteRequest> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleComprobanteRequest> detalles) {
        this.detalles = detalles;
    }

    public List<CampoAdicionalRequest> getCampoAdicional() {
        return campoAdicional;
    }

    public void setCampoAdicional(List<CampoAdicionalRequest> campoAdicional) {
        this.campoAdicional = campoAdicional;
    }
}
