package com.ecommerce.shipping.infrastructure.inbox;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

/**
 * Registro de idempotencia para los mensajes de Kafka ya consumidos. Este es
 * el "inbox" del patrón outbox/inbox: {@code messageId} es el {@code eventId}
 * que viene dentro del mensaje. Si al intentar insertar una fila se produce
 * una violación de clave primaria, significa que este mensaje ya fue
 * procesado antes — es una reentrega de Kafka (que garantiza "al menos una
 * vez", no "exactamente una vez") — y el consumidor puede descartarla con
 * total seguridad, sin volver a aplicar su efecto sobre el negocio.
 */
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
