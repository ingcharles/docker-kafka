package com.facturacion.comprobantes.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "idempotencia_emision")
public class IdempotenciaEmisionEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false)
    public String ruc;

    @Column(nullable = false)
    public String clave;

    @Column(nullable = false)
    public String huella;

    @Column(name = "comprobante_id")
    public UUID comprobanteId;

    @Column(name = "creado_en", nullable = false)
    public Instant creadoEn;
}
