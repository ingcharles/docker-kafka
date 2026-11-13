package com.facturacion.comprobantes.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

/** Una forma de pago del comprobante (tabla {@code comprobante_pago}); ver {@code Pago} en el dominio. */
@Entity
@Table(name = "comprobante_pago")
public class ComprobantePagoEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comprobante_id", nullable = false)
    public ComprobanteEntity comprobante;

    /** Posición dentro del comprobante: preserva el orden en que se enviaron los pagos al generar el XML. */
    @Column(nullable = false)
    public int orden;

    /** Código de la Tabla 24 (FK a {@code forma_pago}). */
    @Column(name = "forma_pago", nullable = false)
    public String formaPago;

    @Column(nullable = false)
    public BigDecimal total;

    /** Solo pago a crédito; va junto con {@link #unidadTiempo}. */
    public BigDecimal plazo;

    @Column(name = "unidad_tiempo")
    public String unidadTiempo;
}
