package com.ecommerce.notification.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "notifications")
public class NotificationEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "order_id", nullable = false)
    public UUID orderId;

    @Column(nullable = false)
    public String type;

    @Column(nullable = false)
    public String recipient;

    @Column(nullable = false)
    public String status;

    @Column(name = "failure_reason")
    public String failureReason;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
