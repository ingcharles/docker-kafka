package com.facturacion.comprobantes.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "punto_emision_local")
public class PuntoEmisionLocalEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false)
    public String ruc;

    @Column(name = "establecimiento_codigo", nullable = false)
    public String establecimientoCodigo;

    @Column(nullable = false)
    public String codigo;

    @Column(nullable = false)
    public boolean activo;

    @Column(name = "actualizado_en", nullable = false)
    public Instant actualizadoEn;
}
