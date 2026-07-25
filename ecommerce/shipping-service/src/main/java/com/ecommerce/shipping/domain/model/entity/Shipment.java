package com.ecommerce.shipping.domain.model.entity;

import java.time.Instant;
import java.util.UUID;

import com.ecommerce.shipping.domain.model.enums.ShipmentStatus;
import com.ecommerce.shipping.domain.model.vo.Address;

public class Shipment {

    private UUID id;
    private UUID orderId;
    private Address address;
    private ShipmentStatus status;
    private String trackingNumber;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;

    public Shipment() {
    }

    public static Shipment createNew(UUID orderId, Address address) {
        Shipment shipment = new Shipment();
        shipment.id = UUID.randomUUID();
        shipment.orderId = orderId;
        shipment.address = address;
        Instant now = Instant.now();
        shipment.createdAt = now;
        shipment.updatedAt = now;
        return shipment;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
