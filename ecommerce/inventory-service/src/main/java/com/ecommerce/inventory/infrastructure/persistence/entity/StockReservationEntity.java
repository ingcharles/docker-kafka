package com.ecommerce.inventory.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "stock_reservations")
public class StockReservationEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "order_id", nullable = false)
    public UUID orderId;

    @Column(name = "product_id", nullable = false)
    public UUID productId;

    @Column(nullable = false)
    public int quantity;

    @Column(nullable = false)
    public String status;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
