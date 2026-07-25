package com.facturacion.orquestacion.infrastructure.inbox;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "processed_message")
public class ProcessedMessageEntity extends PanacheEntityBase {

    @Id
    @Column(name = "message_id")
    public UUID messageId;

    @Column(name = "processed_at", nullable = false)
    public Instant processedAt;

    public static ProcessedMessageEntity of(UUID messageId) {
        ProcessedMessageEntity entity = new ProcessedMessageEntity();
        entity.messageId = messageId;
        entity.processedAt = Instant.now();
        return entity;
    }
}
