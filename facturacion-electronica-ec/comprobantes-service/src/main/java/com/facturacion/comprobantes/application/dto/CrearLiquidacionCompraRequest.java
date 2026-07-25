package com.facturacion.comprobantes.application.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Emitida por el propio comprador cuando el vendedor (proveedor) no puede
 * emitir comprobantes de venta — mismo shape que Factura (detalle,
 * impuestos, totales), solo cambia la contraparte: aquí es un
 * "proveedor" en vez de un "comprador". Se guarda reutilizando las mismas
 * columnas comprador_* del agregado (ver ComprobanteService).
 */
public class CrearLiquidacionCompraRequest {

    @NotNull
    private UUID emisorId;

    @NotBlank
    private String proveedorTipoIdentificacion;

    @NotBlank
    private String proveedorIdentificacion;

    @NotBlank
    private String proveedorRazonSocial;

    private String proveedorDireccion;

    private String proveedorCorreo;

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

    public String getProveedorTipoIdentificacion() {
        return proveedorTipoIdentificacion;
    }

    public void setProveedorTipoIdentificacion(String proveedorTipoIdentificacion) {
        this.proveedorTipoIdentificacion = proveedorTipoIdentificacion;
    }

    public String getProveedorIdentificacion() {
        return proveedorIdentificacion;
    }

    public void setProveedorIdentificacion(String proveedorIdentificacion) {
        this.proveedorIdentificacion = proveedorIdentificacion;
    }

    public String getProveedorRazonSocial() {
        return proveedorRazonSocial;
    }

    public void setProveedorRazonSocial(String proveedorRazonSocial) {
        this.proveedorRazonSocial = proveedorRazonSocial;
    }

    public String getProveedorDireccion() {
        return proveedorDireccion;
    }

    public void setProveedorDireccion(String proveedorDireccion) {
        this.proveedorDireccion = proveedorDireccion;
    }

    public String getProveedorCorreo() {
        return proveedorCorreo;
    }

    public void setProveedorCorreo(String proveedorCorreo) {
        this.proveedorCorreo = proveedorCorreo;
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
