package com.ecommerce.payment.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "payments")
public class PaymentEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    public UUID orderId;

    @Column(nullable = false)
    public BigDecimal amount;

    @Column(nullable = false)
    public String currency;

    @Column(nullable = false)
    public String status;

    @Column(name = "gateway_reference")
    public String gatewayReference;

    @Column(name = "decline_reason")
    public String declineReason;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
