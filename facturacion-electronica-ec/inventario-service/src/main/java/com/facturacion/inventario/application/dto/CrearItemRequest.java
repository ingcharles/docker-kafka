package com.facturacion.inventario.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class CrearItemRequest {

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

    // Clasificación tributaria: solo códigos (Tablas 17 y 18); las tarifas salen del catálogo de comprobantes-service
    // a la fecha de emisión, así un cambio de normativa no obliga a tocar ítems. No hay valor por defecto: la tarifa
    // de IVA cambia con la normativa y no debe quedar escrita en el código.
    @NotBlank
    @Pattern(regexp = "\\d{1,2}", message = "codigoPorcentajeIva debe ser un código de la Tabla 17")
    private String codigoPorcentajeIva;

    /** Código de ICE (Tabla 18) si el ítem lleva ICE; la tarifa sale del catálogo al emitir. */
    @Pattern(regexp = "\\d{4}", message = "codigoIce debe tener 4 dígitos (Tabla 18)")
    private String codigoIce;

    /** Código de IRBPNR (5001) si el ítem lleva el impuesto a las botellas plásticas no retornables. */
    @Pattern(regexp = "\\d{4}", message = "codigoIrbpnr debe tener 4 dígitos (5001)")
    private String codigoIrbpnr;

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
