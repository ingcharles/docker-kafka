package com.ecommerce.order.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "order_items")
public class OrderItemEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    public OrderEntity order;

    @Column(name = "product_id", nullable = false)
    public UUID productId;

    @Column(name = "product_name", nullable = false)
    public String productName;

    @Column(nullable = false)
    public int quantity;

    @Column(name = "unit_price", nullable = false)
    public BigDecimal unitPrice;
}
