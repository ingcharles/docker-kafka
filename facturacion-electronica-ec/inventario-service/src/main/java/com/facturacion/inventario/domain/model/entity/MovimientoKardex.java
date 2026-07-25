package com.facturacion.inventario.domain.model.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.facturacion.inventario.domain.model.enums.TipoMovimientoKardex;

/**
 * Fila del kardex (costeo por promedio ponderado) de un ítem tipo
 * PRODUCTO. Es un registro de solo-anexado (append-only) — nunca se
 * actualiza ni se borra una fila existente; cada movimiento nuevo queda
 * enlazado al saldo que dejó el anterior. Ver
 * {@code domain.service.KardexCalculador} para el algoritmo de costeo.
 */
public class MovimientoKardex {

    private UUID id;
    private UUID itemId;
    private TipoMovimientoKardex tipoMovimiento;
    private BigDecimal cantidad;
    private BigDecimal costoUnitario;
    private BigDecimal cantidadSaldo;
    private BigDecimal costoPromedioSaldo;
    private BigDecimal valorSaldo;
    private String referenciaDocumento;
    private String motivo;
    private Instant creadoEn;

    public MovimientoKardex() {
    }

    public MovimientoKardex(UUID id, UUID itemId, TipoMovimientoKardex tipoMovimiento, BigDecimal cantidad,
            BigDecimal costoUnitario, BigDecimal cantidadSaldo, BigDecimal costoPromedioSaldo,
            BigDecimal valorSaldo, String referenciaDocumento, String motivo, Instant creadoEn) {
        this.id = id;
        this.itemId = itemId;
        this.tipoMovimiento = tipoMovimiento;
        this.cantidad = cantidad;
        this.costoUnitario = costoUnitario;
        this.cantidadSaldo = cantidadSaldo;
        this.costoPromedioSaldo = costoPromedioSaldo;
        this.valorSaldo = valorSaldo;
        this.referenciaDocumento = referenciaDocumento;
        this.motivo = motivo;
        this.creadoEn = creadoEn;
    }

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

    public TipoMovimientoKardex getTipoMovimiento() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(TipoMovimientoKardex tipoMovimiento) {
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
