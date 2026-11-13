package com.facturacion.comprobantes.application.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Una forma de pago (Tabla 24 de la ficha técnica del SRI) — ver {@code Pago} en el dominio. */
public class PagoRequest {

    /**
     * Código de la Tabla 24 del SRI. Aquí solo se valida el formato del XSD (2 dígitos, 01-21); que el código
     * exista y esté vigente lo decide el catálogo de formas de pago (tabla forma_pago) al emitir.
     */
    @NotBlank
    @Pattern(regexp = "^(0[1-9]|1[0-9]|2[01])$", message = "formaPago debe tener el formato del XSD del SRI (01-21)")
    private String formaPago;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal total;

    /** Solo aplica a pago a crédito — opcional; numérico (XSD: decimal 14,2) y va junto con unidadTiempo. */
    @Pattern(regexp = "^\\d{1,12}(\\.\\d{1,2})?$", message = "plazo debe ser numérico (hasta 12 enteros y 2 decimales)")
    private String plazo;

    /** Texto de 1 a 10 caracteres (ej. dias, meses) — solo si se manda plazo, y obligatorio si se manda. */
    @Size(min = 1, max = 10, message = "unidadTiempo debe tener entre 1 y 10 caracteres")
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
