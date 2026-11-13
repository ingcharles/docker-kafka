package com.facturacion.comprobantes.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

/**
 * Un impuesto aplicado a una línea de detalle (tabla {@code detalle_impuesto});
 * ver {@code Impuesto} en el dominio. En Comprobante de Retención cada fila es
 * una línea de retención: {@code codigoPorcentaje} es null y se llenan
 * {@code codigoRetencion} y los {@code docSustento*}.
 */
@Entity
@Table(name = "detalle_impuesto")
public class DetalleImpuestoEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "detalle_id", nullable = false)
    public DetalleComprobanteEntity detalle;

    @Column(nullable = false)
    public int orden;

    @Column(nullable = false)
    public String codigo;

    @Column(name = "codigo_porcentaje")
    public String codigoPorcentaje;

    @Column(nullable = false)
    public BigDecimal tarifa;

    @Column(name = "base_imponible", nullable = false)
    public BigDecimal baseImponible;

    @Column(nullable = false)
    public BigDecimal valor;

    @Column(name = "codigo_retencion")
    public String codigoRetencion;

    @Column(name = "doc_sustento_codigo")
    public String docSustentoCodigo;

    @Column(name = "doc_sustento_numero")
    public String docSustentoNumero;

    @Column(name = "doc_sustento_fecha")
    public LocalDate docSustentoFecha;
}
