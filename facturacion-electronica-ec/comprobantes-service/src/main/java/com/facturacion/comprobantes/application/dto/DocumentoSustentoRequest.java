package com.facturacion.comprobantes.application.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Un {@code docSustento} del Comprobante de Retención v2.0.0 (estilo ATS) —
 * agrupa todas las líneas de retención aplicadas sobre un mismo documento
 * (factura, liquidación de compra, etc.), junto con el impuesto y la forma
 * de pago propios de ESE documento (no de la retención en sí). Puede haber
 * más de uno por comprobante.
 */
public class DocumentoSustentoRequest {

    /** Tabla 5 del catálogo ATS del SRI (ej. "01" crédito tributario). */
    @NotBlank
    private String codSustento;

    /** codDocSustento — Tabla 3. */
    @NotBlank
    private String docSustentoCodigo;

    /** numDocSustento — 15 dígitos sin guiones (estab+ptoEmi+secuencial). */
    @NotBlank
    private String docSustentoNumero;

    /** fechaEmisionDocSustento, dd/mm/aaaa. */
    @NotBlank
    private String docSustentoFecha;

    /** 01=pago local, 02=pago al exterior. */
    @NotBlank
    private String pagoLocExt = "01";

    /** Subtotal sin impuestos del documento sustento (no de esta retención). */
    @NotNull
    private BigDecimal totalSinImpuestos;

    /** Total del documento sustento (no de esta retención). */
    @NotNull
    private BigDecimal importeTotal;

    @NotEmpty
    private List<@Valid ImpuestoDocSustentoRequest> impuestos;

    @NotEmpty
    private List<@Valid RetencionLineaRequest> retenciones;

    /** Forma de pago del documento sustento — si no se manda, se asume "01" (sin utilización del sistema financiero) por el importeTotal. */
    private List<@Valid PagoRequest> pagos;

    public String getCodSustento() {
        return codSustento;
    }

    public void setCodSustento(String codSustento) {
        this.codSustento = codSustento;
    }

    public String getDocSustentoCodigo() {
        return docSustentoCodigo;
    }

    public void setDocSustentoCodigo(String docSustentoCodigo) {
        this.docSustentoCodigo = docSustentoCodigo;
    }

    public String getDocSustentoNumero() {
        return docSustentoNumero;
    }

    public void setDocSustentoNumero(String docSustentoNumero) {
        this.docSustentoNumero = docSustentoNumero;
    }

    public String getDocSustentoFecha() {
        return docSustentoFecha;
    }

    public void setDocSustentoFecha(String docSustentoFecha) {
        this.docSustentoFecha = docSustentoFecha;
    }

    public String getPagoLocExt() {
        return pagoLocExt;
    }

    public void setPagoLocExt(String pagoLocExt) {
        this.pagoLocExt = pagoLocExt;
    }

    public BigDecimal getTotalSinImpuestos() {
        return totalSinImpuestos;
    }

    public void setTotalSinImpuestos(BigDecimal totalSinImpuestos) {
        this.totalSinImpuestos = totalSinImpuestos;
    }

    public BigDecimal getImporteTotal() {
        return importeTotal;
    }

    public void setImporteTotal(BigDecimal importeTotal) {
        this.importeTotal = importeTotal;
    }

    public List<ImpuestoDocSustentoRequest> getImpuestos() {
        return impuestos;
    }

    public void setImpuestos(List<ImpuestoDocSustentoRequest> impuestos) {
        this.impuestos = impuestos;
    }

    public List<RetencionLineaRequest> getRetenciones() {
        return retenciones;
    }

    public void setRetenciones(List<RetencionLineaRequest> retenciones) {
        this.retenciones = retenciones;
    }

    public List<PagoRequest> getPagos() {
        return pagos;
    }

    public void setPagos(List<PagoRequest> pagos) {
        this.pagos = pagos;
    }
}
