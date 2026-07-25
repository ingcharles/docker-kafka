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
@Table(name = "movimiento_kardex")
public class MovimientoKardexEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "item_id", nullable = false)
    public UUID itemId;

    @Column(name = "tipo_movimiento", nullable = false)
    public String tipoMovimiento;

    @Column(nullable = false)
    public BigDecimal cantidad;

    @Column(name = "costo_unitario", nullable = false)
    public BigDecimal costoUnitario;

    @Column(name = "cantidad_saldo", nullable = false)
    public BigDecimal cantidadSaldo;

    @Column(name = "costo_promedio_saldo", nullable = false)
    public BigDecimal costoPromedioSaldo;

    @Column(name = "valor_saldo", nullable = false)
    public BigDecimal valorSaldo;

    @Column(name = "referencia_documento")
    public String referenciaDocumento;

    @Column
    public String motivo;

    @Column(name = "creado_en", nullable = false)
    public Instant creadoEn;
}
