package com.facturacion.firma.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "establecimiento")
public class EstablecimientoEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "empresa_id", nullable = false)
    public UUID empresaId;

    @Column(nullable = false)
    public String codigo;

    @Column(nullable = false)
    public String direccion;

    @Column(nullable = false)
    public String tipo;

    @Column(nullable = false)
    public boolean activo;

    @Column(name = "creado_en", nullable = false)
    public Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    public Instant actualizadoEn;
}
