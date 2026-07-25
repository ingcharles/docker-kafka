package com.facturacion.inventario.domain.model.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.facturacion.inventario.domain.model.enums.EstadoReserva;

public class ReservaStock {

    private UUID id;
    private UUID comprobanteId;
    private UUID itemId;
    private BigDecimal cantidad;
    private EstadoReserva estado;
    private Instant creadoEn;

    public ReservaStock() {
    }

    public ReservaStock(UUID id, UUID comprobanteId, UUID itemId, BigDecimal cantidad, EstadoReserva estado,
            Instant creadoEn) {
        this.id = id;
        this.comprobanteId = comprobanteId;
        this.itemId = itemId;
        this.cantidad = cantidad;
        this.estado = estado;
        this.creadoEn = creadoEn;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getComprobanteId() {
        return comprobanteId;
    }

    public void setComprobanteId(UUID comprobanteId) {
        this.comprobanteId = comprobanteId;
    }

    public UUID getItemId() {
        return itemId;
    }

    public void setItemId(UUID itemId) {
        this.itemId = itemId;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public void setEstado(EstadoReserva estado) {
        this.estado = estado;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(Instant creadoEn) {
        this.creadoEn = creadoEn;
    }
}
