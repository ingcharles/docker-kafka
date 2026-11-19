package com.facturacion.comprobantes.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class DetalleComprobanteRequest {

    /** Opcional — referencia informativa al ítem de inventario-service, si el llamador la tiene. */
    private UUID itemId;

    @NotBlank
    private String codigoPrincipal;

    private String codigoAuxiliar;

    @NotBlank
    private String descripcion;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal cantidad;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal precioUnitario;

    private BigDecimal descuento = BigDecimal.ZERO;

    /** Precio unitario sin subsidio (XSD: {@code precioSinSubsidio}); solo cuando corresponde, p. ej. combustibles subsidiados. */
    @DecimalMin(value = "0.0")
    private BigDecimal precioSinSubsidio;

    /**
     * Opcional cuando {@code itemId} referencia un ítem que ya llegó a la
     * copia local de inventario-service (ver ComprobanteService.construirDetalle):
     * en ese caso el impuesto se deriva automáticamente de la clasificación
     * tributaria del ítem (IVA/ICE) y no hace falta mandarlo. Si se manda de
     * todas formas, tiene prioridad sobre lo que diga el catálogo — así un
     * llamador que no usa inventario-service (o que necesita un impuesto
     * puntual distinto al del catálogo) siempre puede seguir mandándolo a mano.
     */
    private List<@Valid ImpuestoRequest> impuestos;

    /**
     * Detalles adicionales de la línea ({@code <detAdicional nombre valor/>} del XML; la columna "Detalles Adicionales" del
     * RIDE), hasta 3. Si el ítem tiene unidad de medida y no se manda un detalle "Medida", se agrega solo.
     */
    @Size(max = 3, message = "una línea admite como máximo 3 detalles adicionales")
    private List<@Valid CampoAdicionalRequest> detallesAdicionales;

    public List<CampoAdicionalRequest> getDetallesAdicionales() {
        return detallesAdicionales;
    }

    public void setDetallesAdicionales(List<CampoAdicionalRequest> detallesAdicionales) {
        this.detallesAdicionales = detallesAdicionales;
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

    public List<ImpuestoRequest> getImpuestos() {
        return impuestos;
    }

    public void setImpuestos(List<ImpuestoRequest> impuestos) {
        this.impuestos = impuestos;
    }
}
