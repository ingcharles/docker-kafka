package com.facturacion.orquestacion.domain.model.entity;

import java.time.Instant;
import java.util.UUID;

/**
 * Una fila por cada evento recibido o comando enviado — la línea de tiempo
 * de auditoría de la saga. Las filas SENT conservan {@code topic}/{@code payload}
 * (los bytes exactos que se mandaron al outbox) para que
 * {@code POST /sagas/{comprobanteId}/retry} pueda volver a publicar
 * exactamente el mismo contenido — con el mismo eventId incrustado — en
 * vez de generar un comando nuevo que la idempotencia del servicio destino
 * no reconocería como un reintento.
 */
public class SagaStep {

    private UUID id;
    private UUID sagaId;
    private String stepName;
    private String status;
    private String topic;
    private String payload;
    private Instant createdAt;

    public SagaStep() {
    }

    public static SagaStep received(UUID sagaId, String stepName) {
        SagaStep step = new SagaStep();
        step.id = UUID.randomUUID();
        step.sagaId = sagaId;
        step.stepName = stepName;
        step.status = "RECEIVED";
        step.createdAt = Instant.now();
        return step;
    }

    public static SagaStep sent(UUID sagaId, String stepName, String topic, String payload) {
        SagaStep step = new SagaStep();
        step.id = UUID.randomUUID();
        step.sagaId = sagaId;
        step.stepName = stepName;
        step.status = "SENT";
        step.topic = topic;
        step.payload = payload;
        step.createdAt = Instant.now();
        return step;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSagaId() {
        return sagaId;
    }

    public void setSagaId(UUID sagaId) {
        this.sagaId = sagaId;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
