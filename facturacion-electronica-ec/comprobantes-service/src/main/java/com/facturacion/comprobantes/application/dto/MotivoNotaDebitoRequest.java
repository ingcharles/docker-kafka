package com.facturacion.comprobantes.application.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Una línea de {@code <motivos><motivo>} (razón + valor) — Nota de Débito no tiene "detalle" de ítems, solo motivos de cobro. */
public class MotivoNotaDebitoRequest {

    @NotBlank
    private String razon;

    @NotNull
    private BigDecimal valor;

    public String getRazon() {
        return razon;
    }

    public void setRazon(String razon) {
        this.razon = razon;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }
}
