package com.facturacion.inventario.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** No incluye cantidades de stock a propósito: esas solo cambian vía kardex/reservas, nunca por edición directa. */
public class ActualizarItemRequest {

    private UUID categoriaId;

    @NotBlank
    private String descripcion;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal precioUnitario;

    private String unidadMedida;

    private BigDecimal stockMinimo;

    private boolean activo = true;

    @NotBlank
    private String codigoImpuesto;

    @NotBlank
    private String codigoPorcentaje;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal tarifaIva;

    private boolean tieneIce = false;

    private String codigoPorcentajeIce;

    private BigDecimal tarifaIce;

    public UUID getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(UUID categoriaId) {
        this.categoriaId = categoriaId;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public String getUnidadMedida() {
        return unidadMedida;
    }

    public void setUnidadMedida(String unidadMedida) {
        this.unidadMedida = unidadMedida;
    }

    public BigDecimal getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(BigDecimal stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getCodigoImpuesto() {
        return codigoImpuesto;
    }

    public void setCodigoImpuesto(String codigoImpuesto) {
        this.codigoImpuesto = codigoImpuesto;
    }

    public String getCodigoPorcentaje() {
        return codigoPorcentaje;
    }

    public void setCodigoPorcentaje(String codigoPorcentaje) {
        this.codigoPorcentaje = codigoPorcentaje;
    }

    public BigDecimal getTarifaIva() {
        return tarifaIva;
    }

    public void setTarifaIva(BigDecimal tarifaIva) {
        this.tarifaIva = tarifaIva;
    }

    public boolean isTieneIce() {
        return tieneIce;
    }

    public void setTieneIce(boolean tieneIce) {
        this.tieneIce = tieneIce;
    }

    public String getCodigoPorcentajeIce() {
        return codigoPorcentajeIce;
    }

    public void setCodigoPorcentajeIce(String codigoPorcentajeIce) {
        this.codigoPorcentajeIce = codigoPorcentajeIce;
    }

    public BigDecimal getTarifaIce() {
        return tarifaIce;
    }

    public void setTarifaIce(BigDecimal tarifaIce) {
        this.tarifaIce = tarifaIce;
    }
}
