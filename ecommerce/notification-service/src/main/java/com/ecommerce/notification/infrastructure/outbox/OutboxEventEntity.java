package com.ecommerce.notification.infrastructure.outbox;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

/**
 * Fila del patrón Outbox Transaccional. Se escribe dentro de la misma
 * transacción de base de datos que el cambio de negocio que anuncia (ver
 * {@code NotificationSendingService.sendNotification}), y luego un proceso
 * aparte ({@code OutboxRelay}) la publica en Kafka de forma asíncrona. Esto
 * es justamente lo que permite que "la notificación quedó registrada" y "el
 * evento se publicó" sean, en la práctica, una sola operación atómica — sin
 * necesidad de una transacción distribuida (2PC) entre Postgres y Kafka, que
 * además Kafka ni siquiera soporta de forma nativa.
 */
@Entity
@Table(name = "outbox_event")
public class OutboxEventEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "aggregate_id", nullable = false)
    public UUID aggregateId;

    @Column(name = "event_type", nullable = false)
    public String eventType;

    @Column(nullable = false)
    public String topic;

    @Column(nullable = false, columnDefinition = "text")
    public String payload;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "published_at")
    public Instant publishedAt;

    public static OutboxEventEntity of(UUID aggregateId, String eventType, String topic, String payload) {
        OutboxEventEntity entity = new OutboxEventEntity();
        entity.id = UUID.randomUUID();
        entity.aggregateId = aggregateId;
        entity.eventType = eventType;
        entity.topic = topic;
        entity.payload = payload;
        entity.createdAt = Instant.now();
        return entity;
    }
}
