package com.facturacion.comprobantes.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

/** Tabla 6 (tipos de identificación). Solo lectura: {@code PUT /catalogos/tipo_identificacion}. */
@Entity
@Immutable
@Table(name = "tipo_identificacion")
public class TipoIdentificacionEntity extends PanacheEntityBase {

    @Id
    public String codigo;

    public String descripcion;

    @Column(name = "solo_factura")
    public boolean soloFactura;

    public boolean activo;
}
