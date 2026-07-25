package com.facturacion.comprobantes.infrastructure.persistence.entity;

import java.math.BigDecimal;
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

    @Column(name = "codigo_impuesto", nullable = false)
    public String codigoImpuesto;

    @Column(name = "codigo_porcentaje", nullable = false)
    public String codigoPorcentaje;

    @Column(name = "tarifa_iva", nullable = false)
    public BigDecimal tarifaIva;

    @Column(name = "tiene_ice", nullable = false)
    public boolean tieneIce;

    @Column(name = "codigo_porcentaje_ice")
    public String codigoPorcentajeIce;

    @Column(name = "tarifa_ice")
    public BigDecimal tarifaIce;

    @Column(nullable = false)
    public boolean activo;

    @Column(name = "actualizado_en", nullable = false)
    public Instant actualizadoEn;
}
