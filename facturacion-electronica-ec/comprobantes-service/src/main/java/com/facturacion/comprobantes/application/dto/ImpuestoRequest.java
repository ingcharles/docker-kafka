package com.facturacion.comprobantes.application.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * El llamador solo indica qué impuesto aplica y su tarifa (Tabla 16/17 del
 * SRI) — {@code baseImponible} y {@code valor} los calcula el servidor a
 * partir del subtotal de la línea, no se confía en que el cliente los
 * mande ya calculados.
 */
public class ImpuestoRequest {

    @NotBlank
    private String codigo;

    @NotBlank
    private String codigoPorcentaje;

    @NotNull
    private BigDecimal tarifa;

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

    public BigDecimal getTarifa() {
        return tarifa;
    }

    public void setTarifa(BigDecimal tarifa) {
        this.tarifa = tarifa;
    }
}
