package com.ecommerce.shipping.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "shipments")
public class ShipmentEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    public UUID orderId;

    @Column(name = "address_street", nullable = false)
    public String addressStreet;

    @Column(name = "address_city", nullable = false)
    public String addressCity;

    @Column(name = "address_state")
    public String addressState;

    @Column(name = "address_zip")
    public String addressZip;

    @Column(name = "address_country", nullable = false)
    public String addressCountry;

    @Column(nullable = false)
    public String status;

    @Column(name = "tracking_number")
    public String trackingNumber;

    @Column(name = "failure_reason")
    public String failureReason;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
