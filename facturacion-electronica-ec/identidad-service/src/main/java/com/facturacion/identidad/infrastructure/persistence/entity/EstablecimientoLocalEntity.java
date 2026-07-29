package com.facturacion.identidad.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "establecimiento_local")
public class EstablecimientoLocalEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false)
    public String ruc;

    @Column(nullable = false)
    public String codigo;

    @Column(nullable = false)
    public boolean activo;

    @Column(name = "actualizado_en", nullable = false)
    public Instant actualizadoEn;
}
