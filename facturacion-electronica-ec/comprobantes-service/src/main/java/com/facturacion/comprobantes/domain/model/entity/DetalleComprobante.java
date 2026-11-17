package com.facturacion.comprobantes.domain.model.entity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.facturacion.comprobantes.domain.model.vo.CampoAdicional;
import com.facturacion.comprobantes.domain.model.vo.Impuesto;

/** Una línea de {@code <detalle>} — sirve igual para un producto o un servicio, el SRI no distingue entre los dos a este nivel. */
public class DetalleComprobante {

    private UUID id;
    private UUID itemId;
    private String codigoPrincipal;
    private String codigoAuxiliar;
    private String descripcion;
    private BigDecimal cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal descuento;
    private BigDecimal precioSinSubsidio;
    private List<Impuesto> impuestos;
    /** Detalles adicionales de la línea (nombre/valor), hasta 3. */
    private List<CampoAdicional> detallesAdicionales = List.of();

    /** Solo para Guía de Remisión: a qué {@link DestinatarioGuiaRemision} pertenece esta línea. Null en el resto de tipos. */
    private UUID destinatarioId;

    public DetalleComprobante() {
    }

    public DetalleComprobante(UUID id, UUID itemId, String codigoPrincipal, String codigoAuxiliar,
            String descripcion, BigDecimal cantidad, BigDecimal precioUnitario, BigDecimal descuento,
            List<Impuesto> impuestos) {
        this.id = id;
        this.itemId = itemId;
        this.codigoPrincipal = codigoPrincipal;
        this.codigoAuxiliar = codigoAuxiliar;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.descuento = descuento;
        this.impuestos = impuestos;
    }

    /** Tabla 1 del Anexo 1: cantidad * precioUnitario - descuento. */
    public BigDecimal precioTotalSinImpuesto() {
        return cantidad.multiply(precioUnitario).subtract(descuento == null ? BigDecimal.ZERO : descuento);
    }

    public List<CampoAdicional> getDetallesAdicionales() {
        return detallesAdicionales;
    }

    public void setDetallesAdicionales(List<CampoAdicional> detallesAdicionales) {
        this.detallesAdicionales = detallesAdicionales;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getItemId() {
        return itemId;
    }

    public void setItemId(UUID itemId) {
        this.itemId = itemId;
    }

    public String getCodigoPrincipal() {
        return codigoPrincipal;
    }

    public void setCodigoPrincipal(String codigoPrincipal) {
        this.codigoPrincipal = codigoPrincipal;
    }

    public String getCodigoAuxiliar() {
        return codigoAuxiliar;
    }

    public void setCodigoAuxiliar(String codigoAuxiliar) {
        this.codigoAuxiliar = codigoAuxiliar;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal getDescuento() {
        return descuento;
    }

    public void setDescuento(BigDecimal descuento) {
        this.descuento = descuento;
    }

    public BigDecimal getPrecioSinSubsidio() {
        return precioSinSubsidio;
    }

    public void setPrecioSinSubsidio(BigDecimal precioSinSubsidio) {
        this.precioSinSubsidio = precioSinSubsidio;
    }

    public List<Impuesto> getImpuestos() {
        return impuestos;
    }

    public void setImpuestos(List<Impuesto> impuestos) {
        this.impuestos = impuestos;
    }

    public UUID getDestinatarioId() {
        return destinatarioId;
    }

    public void setDestinatarioId(UUID destinatarioId) {
        this.destinatarioId = destinatarioId;
    }
}
