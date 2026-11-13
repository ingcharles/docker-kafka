package com.facturacion.comprobantes.application.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;

/**
 * El llamador indica qué impuesto aplica (Tabla 16/17 del SRI) y, opcionalmente,
 * su tarifa: para IVA la tarifa sale del catálogo (catálogo de este servicio)
 * y si el cliente la manda debe coincidir con ella; ICE/IRBPNR no tienen tarifas
 * en el catálogo, ahí la tarifa es obligatoria — {@code baseImponible} y {@code valor} los calcula el servidor a
 * partir del subtotal de la línea, no se confía en que el cliente los
 * mande ya calculados.
 */
public class ImpuestoRequest {

    @NotBlank
    private String codigo;

    @NotBlank
    private String codigoPorcentaje;

    /** Opcional para IVA (se toma del catálogo); obligatoria para ICE/IRBPNR. */
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
