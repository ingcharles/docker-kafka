package com.facturacion.firma.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "configuracion_emisor")
public class ConfiguracionEmisorEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "emisor_id", nullable = false)
    public UUID emisorId;

    @Column(nullable = false)
    public String clave;

    @Column(nullable = false, columnDefinition = "text")
    public String valor;

    @Column(name = "creado_en", nullable = false)
    public Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    public Instant actualizadoEn;
}
