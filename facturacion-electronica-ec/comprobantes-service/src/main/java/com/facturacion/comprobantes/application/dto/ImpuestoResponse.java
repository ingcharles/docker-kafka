package com.facturacion.comprobantes.application.dto;

import java.math.BigDecimal;

public class ImpuestoResponse {

    private String codigo;
    private String codigoPorcentaje;
    private BigDecimal tarifa;
    private BigDecimal baseImponible;
    private BigDecimal valor;
    private String codigoRetencion;
    private String docSustentoCodigo;
    private String docSustentoNumero;
    private String docSustentoFecha;

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

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getCodigoRetencion() {
        return codigoRetencion;
    }

    public void setCodigoRetencion(String codigoRetencion) {
        this.codigoRetencion = codigoRetencion;
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
}
