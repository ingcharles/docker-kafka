package com.facturacion.inventario.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

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
    @Pattern(regexp = "\\d{1,2}", message = "codigoPorcentajeIva debe ser un código de la Tabla 17")
    private String codigoPorcentajeIva;

    /** Código de ICE (Tabla 18) si el ítem lleva ICE; la tarifa sale del catálogo al emitir. */
    @Pattern(regexp = "\\d{4}", message = "codigoIce debe tener 4 dígitos (Tabla 18)")
    private String codigoIce;

    /** Código de IRBPNR (5001) si el ítem lleva el impuesto a las botellas plásticas no retornables. */
    @Pattern(regexp = "\\d{4}", message = "codigoIrbpnr debe tener 4 dígitos (5001)")
    private String codigoIrbpnr;

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

    public String getCodigoPorcentajeIva() {
        return codigoPorcentajeIva;
    }

    public void setCodigoPorcentajeIva(String codigoPorcentajeIva) {
        this.codigoPorcentajeIva = codigoPorcentajeIva;
    }

    public String getCodigoIce() {
        return codigoIce;
    }

    public void setCodigoIce(String codigoIce) {
        this.codigoIce = codigoIce;
    }

    public String getCodigoIrbpnr() {
        return codigoIrbpnr;
    }

    public void setCodigoIrbpnr(String codigoIrbpnr) {
        this.codigoIrbpnr = codigoIrbpnr;
    }
}
