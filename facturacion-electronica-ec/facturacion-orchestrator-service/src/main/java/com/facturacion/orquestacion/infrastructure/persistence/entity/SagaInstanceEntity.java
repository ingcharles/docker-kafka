package com.facturacion.orquestacion.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "saga_instance")
public class SagaInstanceEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false)
    public String state;

    @Column(name = "empresa_ruc", nullable = false)
    public String empresaRuc;

    @Column(name = "clave_acceso", nullable = false)
    public String claveAcceso;

    @Column(nullable = false)
    public String ambiente;

    @Column(name = "failure_reason")
    public String failureReason;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @Column(nullable = false)
    public int reintentos;

    @Column(name = "notificacion_esperada", nullable = false)
    public boolean notificacionEsperada = true;

    @Column(name = "usa_stock", nullable = false)
    public boolean usaStock;
}
