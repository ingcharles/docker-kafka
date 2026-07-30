package com.facturacion.comprobantes.application.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * El impuesto del documento sustento de una retención (XSD v2.0.0,
 * {@code impuestosDocSustento/impuestoDocSustento}) — el IVA/ICE que ya
 * traía la factura/comprobante sobre el que se retiene, no el valor
 * retenido en sí (eso es {@link RetencionLineaRequest}). El llamador manda
 * {@code baseImponible}/{@code valor} directos porque este comprobante no
 * tiene un detalle propio del que derivarlos.
 */
public class ImpuestoDocSustentoRequest {

    /** codImpuestoDocSustento — Tabla 16/18 (2=IVA, 3=ICE, 5=IRBPNR). */
    @NotBlank
    private String codigo;

    /** Tabla 17 para IVA. */
    @NotBlank
    private String codigoPorcentaje;

    @NotNull
    private BigDecimal baseImponible;

    @NotNull
    private BigDecimal tarifa;

    @NotNull
    private BigDecimal valor;

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigoPorcentaje() {
        return codigoPorcentaje;
    }

    public void setCodigoPorcentaje(String codigoPorcentaje) {
        this.codigoPorcentaje = codigoPorcentaje;
    }

    public BigDecimal getBaseImponible() {
        return baseImponible;
    }

    public void setBaseImponible(BigDecimal baseImponible) {
        this.baseImponible = baseImponible;
    }

    public BigDecimal getTarifa() {
        return tarifa;
    }

    public void setTarifa(BigDecimal tarifa) {
        this.tarifa = tarifa;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }
}
