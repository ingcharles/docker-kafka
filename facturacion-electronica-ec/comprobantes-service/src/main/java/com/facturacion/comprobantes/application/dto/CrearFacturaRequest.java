package com.facturacion.comprobantes.application.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * API genérica a propósito: no distingue si los ítems facturados son
 * productos o servicios (el SRI tampoco lo hace a este nivel) — cualquier
 * sistema externo (ecommerce, POS, un ERP) puede llamar esto con solo los
 * datos de la venta, sin conocer nada de inventario-service.
 */
public class CrearFacturaRequest {

    @NotNull
    private UUID emisorId;

    @NotBlank
    private String compradorTipoIdentificacion;

    @NotBlank
    private String compradorIdentificacion;

    @NotBlank
    private String compradorRazonSocial;

    private String compradorDireccion;

    /** A dónde notificacion-service manda el comprobante autorizado — opcional, si no se da no se notifica por correo. */
    private String compradorCorreo;

    private String moneda = "DOLAR";

    @NotEmpty
    @Valid
    private List<DetalleComprobanteRequest> detalles;

    public UUID getEmisorId() {
        return emisorId;
    }

    public void setEmisorId(UUID emisorId) {
        this.emisorId = emisorId;
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
}
