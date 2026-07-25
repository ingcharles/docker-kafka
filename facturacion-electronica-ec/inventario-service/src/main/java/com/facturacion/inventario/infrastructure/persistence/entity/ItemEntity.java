package com.facturacion.inventario.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "item")
public class ItemEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false)
    public String ruc;

    @Column(nullable = false)
    public String tipo;

    @Column(name = "categoria_id")
    public UUID categoriaId;

    @Column(name = "codigo_principal", nullable = false)
    public String codigoPrincipal;

    @Column(name = "codigo_auxiliar")
    public String codigoAuxiliar;

    @Column(nullable = false)
    public String descripcion;

    @Column(name = "precio_unitario", nullable = false)
    public BigDecimal precioUnitario;

    @Column(name = "unidad_medida")
    public String unidadMedida;

    @Column(name = "cantidad_disponible", nullable = false)
    public BigDecimal cantidadDisponible;

    @Column(name = "cantidad_reservada", nullable = false)
    public BigDecimal cantidadReservada;

    @Column(name = "stock_minimo", nullable = false)
    public BigDecimal stockMinimo;

    @Column(nullable = false)
    public boolean activo;

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

    @Column(name = "creado_en", nullable = false)
    public Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    public Instant actualizadoEn;
}
