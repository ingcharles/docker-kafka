package com.facturacion.identidad.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "empresa_local")
public class EmpresaLocalEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false, unique = true)
    public String ruc;

    @Column(name = "razon_social", nullable = false)
    public String razonSocial;

    @Column(nullable = false)
    public boolean activo;

    @Column(name = "actualizado_en", nullable = false)
    public Instant actualizadoEn;
}
