package com.facturacion.comprobantes.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "item_local")
public class ItemLocalEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false)
    public String ruc;

    @Column(name = "codigo_principal", nullable = false)
    public String codigoPrincipal;

    @Column(nullable = false)
    public String descripcion;

    @Column(name = "unidad_medida")
    public String unidadMedida;

    @Column(name = "codigo_porcentaje_iva", nullable = false)
    public String codigoPorcentajeIva;

    @Column(name = "codigo_ice")
    public String codigoIce;

    @Column(name = "codigo_irbpnr")
    public String codigoIrbpnr;

    @Column(nullable = false)
    public boolean activo;

    @Column(name = "actualizado_en", nullable = false)
    public Instant actualizadoEn;
}
