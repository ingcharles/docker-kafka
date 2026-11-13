package com.facturacion.comprobantes.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

/** Sustento tributario (catálogo del ATS). Solo lectura: {@code PUT /catalogos/sustento_tributario}. */
@Entity
@Immutable
@Table(name = "sustento_tributario")
public class SustentoTributarioEntity extends PanacheEntityBase {

    @Id
    public String codigo;

    public String descripcion;

    public boolean activo;
}
