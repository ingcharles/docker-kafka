package com.ecommerce.order.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "orders")
public class OrderEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "customer_id", nullable = false)
    public String customerId;

    @Column(nullable = false)
    public String status;

    @Column(nullable = false)
    public String currency;

    @Column(name = "total_amount", nullable = false)
    public BigDecimal totalAmount;

    @Column(name = "shipping_street", nullable = false)
    public String shippingStreet;

    @Column(name = "shipping_city", nullable = false)
    public String shippingCity;

    @Column(name = "shipping_state")
    public String shippingState;

    @Column(name = "shipping_zip")
    public String shippingZip;

    @Column(name = "shipping_country", nullable = false)
    public String shippingCountry;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    // EAGER a propósito: todos los caminos de lectura (findById, findPaged) necesitan
    // los items, y Hibernate Reactive no permite el lazy-load implícito fuera de su
    // sesión dueña (no existe el N+1 transparente que sí tiene Hibernate en modo
    // bloqueante) — la alternativa sería usar "fetch join" en cada consulta, pero
    // para una colección tan pequeña como esta, EAGER es más simple.
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("productName")
    public List<OrderItemEntity> items = new ArrayList<>();
}
