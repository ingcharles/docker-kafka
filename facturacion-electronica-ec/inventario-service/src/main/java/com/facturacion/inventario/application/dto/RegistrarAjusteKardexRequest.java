package com.facturacion.inventario.application.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RegistrarAjusteKardexRequest {

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal cantidad;

    /** true = ajuste positivo (se encontró más stock del registrado); false = negativo (merma, pérdida). */
    private boolean positivo;

    @NotBlank
    private String motivo;

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public boolean isPositivo() {
        return positivo;
    }

    public void setPositivo(boolean positivo) {
        this.positivo = positivo;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
