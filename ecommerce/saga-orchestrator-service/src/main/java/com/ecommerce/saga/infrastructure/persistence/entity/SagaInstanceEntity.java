package com.ecommerce.saga.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "saga_instance")
public class SagaInstanceEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false)
    public String state;

    @Column(name = "customer_id", nullable = false)
    public String customerId;

    @Column(name = "total_amount", nullable = false)
    public BigDecimal totalAmount;

    @Column(nullable = false)
    public String currency;

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

    @Column(name = "items_snapshot", nullable = false, columnDefinition = "text")
    public String itemsSnapshot;

    @Column(name = "failure_reason")
    public String failureReason;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
