package com.ecommerce.inventory.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "products", uniqueConstraints = @UniqueConstraint(columnNames = "sku"))
public class ProductEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false)
    public String sku;

    @Column(nullable = false)
    public String name;

    @Column(name = "unit_price", nullable = false)
    public BigDecimal unitPrice;

    @Column(name = "available_quantity", nullable = false)
    public int availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    public int reservedQuantity;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
