package com.facturacion.identidad.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "api_key")
public class ApiKeyEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "usuario_id", nullable = false)
    public UUID usuarioId;

    @Column(nullable = false)
    public String nombre;

    @Column(nullable = false)
    public String ruc;

    @Column(nullable = false)
    public String rol;

    public String establecimiento;

    @Column(name = "punto_emision")
    public String puntoEmision;

    @Column(nullable = false, unique = true)
    public String jti;

    @Column(name = "fecha_creacion", nullable = false)
    public Instant fechaCreacion;

    @Column(name = "fecha_expiracion")
    public Instant fechaExpiracion;

    @Column(nullable = false)
    public boolean revocada;

    @Column(name = "revocada_en")
    public Instant revocadaEn;
}
