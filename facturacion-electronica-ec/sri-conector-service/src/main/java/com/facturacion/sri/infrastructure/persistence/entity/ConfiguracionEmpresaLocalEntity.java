package com.facturacion.sri.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "configuracion_empresa_local")
public class ConfiguracionEmpresaLocalEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false)
    public String ruc;

    @Column(nullable = false)
    public String clave;

    @Column(columnDefinition = "text")
    public String valor;

    @Column(name = "actualizado_en", nullable = false)
    public Instant actualizadoEn;
}
