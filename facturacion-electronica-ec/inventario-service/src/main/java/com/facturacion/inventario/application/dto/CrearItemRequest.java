package com.facturacion.inventario.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.facturacion.inventario.domain.model.enums.ImpuestoSri;

public class CrearItemRequest {

    /** Código de porcentaje de la Tabla 17 que se asume si el cliente no manda uno (IVA 15%); debe existir en el catálogo. */
    private static final String CODIGO_PORCENTAJE_IVA_POR_DEFECTO = "4";

    /** RUC de la empresa dueña de este ítem — el catálogo es multi-tenant (ver ARQUITECTURA.md). */
    @NotBlank
    @Pattern(regexp = "\\d{13}", message = "el RUC debe tener 13 dígitos")
    private String ruc;

    @NotNull
    private String tipo;

    private UUID categoriaId;

    @NotBlank
    private String codigoPrincipal;

    private String codigoAuxiliar;

    @NotBlank
    private String descripcion;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal precioUnitario;

    private String unidadMedida;

    /** Solo aplica si tipo=PRODUCTO. Si es mayor a cero, genera el primer movimiento de kardex (ENTRADA). */
    private BigDecimal cantidadInicial = BigDecimal.ZERO;

    /** Costo unitario de la entrada inicial — obligatorio si cantidadInicial > 0. */
    private BigDecimal costoInicial;

    private BigDecimal stockMinimo = BigDecimal.ZERO;

    // Clasificación tributaria — default IVA 15% (código de porcentaje 4,
    // Tabla 17 de la ficha técnica del SRI), el caso más común; se puede
    // sobreescribir para ítems con otra tarifa (0%, exento, etc.) o con ICE.
    @NotBlank
    private String codigoImpuesto = ImpuestoSri.IVA.codigo();

    @NotBlank
    private String codigoPorcentaje = CODIGO_PORCENTAJE_IVA_POR_DEFECTO;

    private boolean tieneIce = false;

    private String codigoPorcentajeIce;

    private BigDecimal tarifaIce;

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public UUID getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(UUID categoriaId) {
        this.categoriaId = categoriaId;
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

    public BigDecimal getCantidadInicial() {
        return cantidadInicial;
    }

    public void setCantidadInicial(BigDecimal cantidadInicial) {
        this.cantidadInicial = cantidadInicial;
    }

    public BigDecimal getCostoInicial() {
        return costoInicial;
    }

    public void setCostoInicial(BigDecimal costoInicial) {
        this.costoInicial = costoInicial;
    }

    public BigDecimal getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(BigDecimal stockMinimo) {
        this.stockMinimo = stockMinimo;
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
