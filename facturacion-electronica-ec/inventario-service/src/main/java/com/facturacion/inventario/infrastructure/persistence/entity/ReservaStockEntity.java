package com.facturacion.inventario.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "reserva_stock")
public class ReservaStockEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "comprobante_id", nullable = false)
    public UUID comprobanteId;

    @Column(name = "item_id", nullable = false)
    public UUID itemId;

    @Column(nullable = false)
    public BigDecimal cantidad;

    @Column(nullable = false)
    public String estado;

    @Column(name = "creado_en", nullable = false)
    public Instant creadoEn;
}
