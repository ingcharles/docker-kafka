package com.facturacion.comprobantes.application.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Una forma de pago (Tabla 24 de la ficha técnica del SRI) — ver {@code Pago} en el dominio. */
public class PagoRequest {

    @NotBlank
    private String formaPago;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal total;

    /** Solo aplica a pago a crédito — opcional. */
    private String plazo;

    /** dias|meses|anios — solo si se manda plazo. */
    private String unidadTiempo;

    public String getFormaPago() {
        return formaPago;
    }

    public void setFormaPago(String formaPago) {
        this.formaPago = formaPago;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getPlazo() {
        return plazo;
    }

    public void setPlazo(String plazo) {
        this.plazo = plazo;
    }

    public String getUnidadTiempo() {
        return unidadTiempo;
    }

    public void setUnidadTiempo(String unidadTiempo) {
        this.unidadTiempo = unidadTiempo;
    }
}
