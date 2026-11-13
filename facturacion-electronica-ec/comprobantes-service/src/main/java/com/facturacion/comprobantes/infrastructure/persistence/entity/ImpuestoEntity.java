package com.facturacion.comprobantes.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

/** Tabla 16 (impuestos: IVA, ICE, IRBPNR). Solo lectura: se modifica con {@code PUT /catalogos/impuesto}. */
@Entity
@Immutable
@Table(name = "impuesto")
public class ImpuestoEntity extends PanacheEntityBase {

    @Id
    public String codigo;

    public String descripcion;

    public boolean activo;
}
