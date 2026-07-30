package com.facturacion.comprobantes.application.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Una línea {@code <retencion>} dentro de un {@link DocumentoSustentoRequest}
 * (XSD v2.0.0) — a diferencia de Factura, aquí cada línea ya trae
 * {@code baseImponible} y {@code valorRetenido} calculados por el llamador
 * (el servidor no los recalcula: la retención se aplica sobre el documento
 * sustento, no sobre un detalle propio de este comprobante). La referencia
 * al documento sustento (código/número/fecha) vive en el
 * {@code DocumentoSustentoRequest} padre, no aquí — un mismo documento
 * puede tener varias líneas de retención (ej. IVA + Renta).
 */
public class RetencionLineaRequest {

    @NotBlank
    private String codigo;

    /** Tabla 20/21 de la ficha técnica. */
    @NotBlank
    private String codigoRetencion;

    @NotNull
    private BigDecimal baseImponible;

    @NotNull
    private BigDecimal porcentajeRetener;

    @NotNull
    private BigDecimal valorRetenido;

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigoRetencion() {
        return codigoRetencion;
    }

    public void setCodigoRetencion(String codigoRetencion) {
        this.codigoRetencion = codigoRetencion;
    }

    public BigDecimal getBaseImponible() {
        return baseImponible;
    }

    public void setBaseImponible(BigDecimal baseImponible) {
        this.baseImponible = baseImponible;
    }

    public BigDecimal getPorcentajeRetener() {
        return porcentajeRetener;
    }

    public void setPorcentajeRetener(BigDecimal porcentajeRetener) {
        this.porcentajeRetener = porcentajeRetener;
    }

    public BigDecimal getValorRetenido() {
        return valorRetenido;
    }

    public void setValorRetenido(BigDecimal valorRetenido) {
        this.valorRetenido = valorRetenido;
    }
}
