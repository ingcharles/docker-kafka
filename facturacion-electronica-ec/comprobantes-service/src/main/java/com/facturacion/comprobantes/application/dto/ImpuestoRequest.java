package com.facturacion.comprobantes.application.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;

import com.facturacion.comprobantes.domain.model.enums.TipoTarifa;

/**
 * El llamador indica qué impuesto aplica (Tabla 16/17/18 del SRI) y, opcionalmente, su tarifa: sale del catálogo
 * (IVA, ICE e IRBPNR) y si el cliente la manda debe coincidir con ella. El {@code valor} lo calcula siempre el
 * servidor; la base imponible también, salvo la del ICE y el IRBPNR, que el emisor puede indicar.
 */
public class ImpuestoRequest {

    @NotBlank
    private String codigo;

    @NotBlank
    private String codigoPorcentaje;

    /** Opcional: se toma del catálogo (Tablas 17 y 18); si el catálogo del impuesto está vacío es obligatoria. */
    private BigDecimal tarifa;

    /**
     * Solo ICE e IRBPNR (Tabla 21: "editable por la naturaleza del cálculo"): la base imponible cuando no es el subtotal
     * de la línea. Con tarifa porcentual, la base por PVP o precio de fábrica (art. 76 LRTI); con tarifa específica, las
     * unidades (litros de alcohol puro, 100 g de azúcar...). Sin ella: subtotal de la línea o cantidad, respectivamente.
     */
    private BigDecimal baseImponible;

    /**
     * Solo si el código tiene las dos modalidades en la Tabla 18 (p. ej. ICE 3031: 75 % y USD 10,00). Sin valor se usa
     * la porcentual.
     */
    private TipoTarifa tipoTarifa;

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

    public BigDecimal getBaseImponible() {
        return baseImponible;
    }

    public void setBaseImponible(BigDecimal baseImponible) {
        this.baseImponible = baseImponible;
    }

    public TipoTarifa getTipoTarifa() {
        return tipoTarifa;
    }

    public void setTipoTarifa(TipoTarifa tipoTarifa) {
        this.tipoTarifa = tipoTarifa;
    }
}
