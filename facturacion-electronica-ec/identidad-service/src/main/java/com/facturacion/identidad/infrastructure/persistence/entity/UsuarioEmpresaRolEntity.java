package com.facturacion.identidad.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "usuario_empresa_rol")
public class UsuarioEmpresaRolEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "usuario_id", nullable = false)
    public UUID usuarioId;

    @Column(nullable = false)
    public String ruc;

    @Column(nullable = false)
    public String rol;

    @Column(name = "creado_en", nullable = false)
    public Instant creadoEn;
}
