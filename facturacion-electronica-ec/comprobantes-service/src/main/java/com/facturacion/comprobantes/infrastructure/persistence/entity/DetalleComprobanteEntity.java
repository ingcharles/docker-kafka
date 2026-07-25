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

@Entity
@Table(name = "detalle_comprobante")
public class DetalleComprobanteEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comprobante_id", nullable = false)
    public ComprobanteEntity comprobante;

    @Column(name = "item_id")
    public UUID itemId;

    @Column(name = "codigo_principal", nullable = false)
    public String codigoPrincipal;

    @Column(name = "codigo_auxiliar")
    public String codigoAuxiliar;

    @Column(nullable = false)
    public String descripcion;

    @Column(nullable = false)
    public BigDecimal cantidad;

    @Column(name = "precio_unitario", nullable = false)
    public BigDecimal precioUnitario;

    @Column(nullable = false)
    public BigDecimal descuento;

    @Column(name = "precio_total_sin_impuesto", nullable = false)
    public BigDecimal precioTotalSinImpuesto;

    /** Arreglo de impuestos de esta línea (código, código de porcentaje, tarifa, base, valor), serializado como JSON. */
    @Column(name = "impuestos_json", nullable = false, columnDefinition = "text")
    public String impuestosJson;

    /**
     * Solo para Guía de Remisión: id del {@link DestinatarioGuiaRemisionEntity}
     * al que pertenece esta línea. Se guarda como columna simple (no
     * {@code @ManyToOne}) porque nada en este servicio necesita navegar de
     * detalle -> destinatario como grafo de objetos, solo agrupar por id al
     * generar el XML (ver GeneradorXmlGuiaRemision).
     */
    @Column(name = "destinatario_id")
    public UUID destinatarioId;
}
