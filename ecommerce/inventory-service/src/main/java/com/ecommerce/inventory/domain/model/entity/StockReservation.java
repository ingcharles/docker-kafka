package com.ecommerce.inventory.domain.model.entity;

import java.time.Instant;
import java.util.UUID;

import com.ecommerce.inventory.domain.model.enums.ReservationStatus;

/** Una línea de stock retenida para una orden — una fila por cada combinación (orderId, productId). */
public class StockReservation {

    private UUID id;
    private UUID orderId;
    private UUID productId;
    private int quantity;
    private ReservationStatus status;
    private Instant createdAt;

    public StockReservation() {
    }

    public static StockReservation reservedFor(UUID orderId, UUID productId, int quantity) {
        StockReservation reservation = new StockReservation();
        reservation.id = UUID.randomUUID();
        reservation.orderId = orderId;
        reservation.productId = productId;
        reservation.quantity = quantity;
        reservation.status = ReservationStatus.RESERVED;
        reservation.createdAt = Instant.now();
        return reservation;
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

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
