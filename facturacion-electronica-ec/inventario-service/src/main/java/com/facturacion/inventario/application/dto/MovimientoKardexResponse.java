package com.facturacion.inventario.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class MovimientoKardexResponse {

    private UUID id;
    private UUID itemId;
    private String tipoMovimiento;
    private BigDecimal cantidad;
    private BigDecimal costoUnitario;
    private BigDecimal cantidadSaldo;
    private BigDecimal costoPromedioSaldo;
    private BigDecimal valorSaldo;
    private String referenciaDocumento;
    private String motivo;
    private Instant creadoEn;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getItemId() {
        return itemId;
    }

    public void setItemId(UUID itemId) {
        this.itemId = itemId;
    }

    public String getTipoMovimiento() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(String tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public void setCostoUnitario(BigDecimal costoUnitario) {
        this.costoUnitario = costoUnitario;
    }

    public BigDecimal getCantidadSaldo() {
        return cantidadSaldo;
    }

    public void setCantidadSaldo(BigDecimal cantidadSaldo) {
        this.cantidadSaldo = cantidadSaldo;
    }

    public BigDecimal getCostoPromedioSaldo() {
        return costoPromedioSaldo;
    }

    public void setCostoPromedioSaldo(BigDecimal costoPromedioSaldo) {
        this.costoPromedioSaldo = costoPromedioSaldo;
    }

    public BigDecimal getValorSaldo() {
        return valorSaldo;
    }

    public void setValorSaldo(BigDecimal valorSaldo) {
        this.valorSaldo = valorSaldo;
    }

    public String getReferenciaDocumento() {
        return referenciaDocumento;
    }

    public void setReferenciaDocumento(String referenciaDocumento) {
        this.referenciaDocumento = referenciaDocumento;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(Instant creadoEn) {
        this.creadoEn = creadoEn;
    }
}
