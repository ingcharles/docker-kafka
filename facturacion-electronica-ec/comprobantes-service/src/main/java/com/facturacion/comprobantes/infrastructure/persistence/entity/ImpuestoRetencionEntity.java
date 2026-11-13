package com.facturacion.comprobantes.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

/** Tabla 19 (impuestos a retener: Renta, IVA, ISD). Solo lectura: {@code PUT /catalogos/impuesto_retencion}. */
@Entity
@Immutable
@Table(name = "impuesto_retencion")
public class ImpuestoRetencionEntity extends PanacheEntityBase {

    @Id
    public String codigo;

    public String descripcion;

    public boolean activo;
}
