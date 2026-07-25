package com.facturacion.orquestacion.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "saga_step")
public class SagaStepEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "saga_id", nullable = false)
    public UUID sagaId;

    @Column(name = "step_name", nullable = false)
    public String stepName;

    @Column(nullable = false)
    public String status;

    @Column
    public String topic;

    @Column(columnDefinition = "text")
    public String payload;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
