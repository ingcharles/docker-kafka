package com.ecommerce.notification.domain.model.entity;

import java.time.Instant;
import java.util.UUID;

import com.ecommerce.notification.domain.model.enums.NotificationStatus;

public class Notification {

    private UUID id;
    private UUID orderId;
    private String type;
    private String recipient;
    private NotificationStatus status;
    private String failureReason;
    private Instant createdAt;

    public Notification() {
    }

    public static Notification createNew(UUID orderId, String type, String recipient) {
        Notification notification = new Notification();
        notification.id = UUID.randomUUID();
        notification.orderId = orderId;
        notification.type = type;
        notification.recipient = recipient;
        notification.createdAt = Instant.now();
        return notification;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
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
}
