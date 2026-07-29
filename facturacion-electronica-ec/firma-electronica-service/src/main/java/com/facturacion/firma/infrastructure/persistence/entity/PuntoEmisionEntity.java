package com.facturacion.firma.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "punto_emision")
public class PuntoEmisionEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "establecimiento_id", nullable = false)
    public UUID establecimientoId;

    @Column(nullable = false)
    public String codigo;

    public String descripcion;

    @Column(nullable = false)
    public boolean activo;

    @Column(name = "creado_en", nullable = false)
    public Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    public Instant actualizadoEn;
}
