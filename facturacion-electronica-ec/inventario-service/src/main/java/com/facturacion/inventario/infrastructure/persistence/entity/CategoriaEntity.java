package com.facturacion.inventario.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "categoria")
public class CategoriaEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false)
    public String nombre;

    @Column(nullable = false)
    public String tipo;

    @Column
    public String descripcion;

    @Column(nullable = false)
    public boolean activa;

    @Column(name = "creado_en", nullable = false)
    public Instant creadoEn;
}
