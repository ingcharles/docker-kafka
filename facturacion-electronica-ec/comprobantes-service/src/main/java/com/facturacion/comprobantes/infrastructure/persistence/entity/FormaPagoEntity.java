package com.facturacion.comprobantes.infrastructure.persistence.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

/** Tabla 24 de la ficha técnica (formas de pago). Solo lectura: se modifica con {@code PUT /catalogos/forma_pago}. */
@Entity
@Immutable
@Table(name = "forma_pago")
public class FormaPagoEntity extends PanacheEntityBase {

    @Id
    public String codigo;

    public String descripcion;

    @Column(name = "vigente_desde")
    public LocalDate vigenteDesde;

    @Column(name = "vigente_hasta")
    public LocalDate vigenteHasta;

    public boolean activo;
}
