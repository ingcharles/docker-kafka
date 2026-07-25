package com.facturacion.comprobantes.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "comprobante")
public class ComprobanteEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "tipo_comprobante", nullable = false)
    public String tipoComprobante;

    @Column(name = "clave_acceso", nullable = false, unique = true)
    public String claveAcceso;

    @Column(nullable = false)
    public String ambiente;

    @Column(nullable = false)
    public String establecimiento;

    @Column(name = "punto_emision", nullable = false)
    public String puntoEmision;

    @Column(nullable = false)
    public String secuencial;

    @Column(name = "emisor_ruc", nullable = false)
    public String emisorRuc;

    @Column(name = "emisor_razon_social", nullable = false)
    public String emisorRazonSocial;

    @Column(name = "emisor_nombre_comercial")
    public String emisorNombreComercial;

    @Column(name = "emisor_direccion_matriz", nullable = false)
    public String emisorDireccionMatriz;

    @Column(name = "emisor_obligado_contabilidad", nullable = false)
    public boolean emisorObligadoContabilidad;

    @Column(name = "emisor_contribuyente_especial", nullable = false)
    public boolean emisorContribuyenteEspecial;

    @Column(name = "emisor_numero_contribuyente_especial")
    public String emisorNumeroContribuyenteEspecial;

    @Column(name = "emisor_es_gran_contribuyente", nullable = false)
    public boolean emisorEsGranContribuyente;

    @Column(name = "emisor_numero_resolucion_gran_contribuyente")
    public String emisorNumeroResolucionGranContribuyente;

    @Column(name = "fecha_emision", nullable = false)
    public Instant fechaEmision;

    // Sin nullable=false a propósito: Guía de Remisión no tiene comprador
    // (es un traslado de bienes, no una venta) — ver V2__tipos_comprobante_adicionales.sql.
    @Column(name = "comprador_tipo_identificacion")
    public String compradorTipoIdentificacion;

    @Column(name = "comprador_identificacion")
    public String compradorIdentificacion;

    @Column(name = "comprador_razon_social")
    public String compradorRazonSocial;

    @Column(name = "comprador_direccion")
    public String compradorDireccion;

    @Column(name = "comprador_correo")
    public String compradorCorreo;

    @Column(name = "subtotal_sin_impuestos", nullable = false)
    public BigDecimal subtotalSinImpuestos;

    @Column(name = "total_descuento", nullable = false)
    public BigDecimal totalDescuento;

    @Column(name = "importe_total", nullable = false)
    public BigDecimal importeTotal;

    @Column(nullable = false)
    public String moneda;

    @Column(nullable = false)
    public String estado;

    @Column(name = "xml_generado", nullable = false, columnDefinition = "text")
    public String xmlGenerado;

    @Column(name = "xml_firmado", columnDefinition = "text")
    public String xmlFirmado;

    @Column(name = "xml_autorizado", columnDefinition = "text")
    public String xmlAutorizado;

    @Column(name = "numero_autorizacion")
    public String numeroAutorizacion;

    @Column(name = "fecha_autorizacion")
    public Instant fechaAutorizacion;

    @Column(name = "motivo_rechazo")
    public String motivoRechazo;

    // Sin @Lob a propósito (columna bytea, no LOB real) — ver la misma nota
    // en ImagenItemEntity de inventario-service; con @Lob, Hibernate lo
    // envuelve en un BlobProxy que el cliente reactivo no puede bindear.
    @Column(name = "ride_pdf")
    public byte[] ridePdf;

    @Column(name = "direccion_establecimiento")
    public String direccionEstablecimiento;

    @Column(name = "doc_modificado_tipo")
    public String docModificadoTipo;

    @Column(name = "doc_modificado_numero")
    public String docModificadoNumero;

    @Column(name = "doc_modificado_fecha")
    public Instant docModificadoFecha;

    @Column
    public String motivo;

    @Column(name = "periodo_fiscal")
    public String periodoFiscal;

    @Column(name = "transportista_razon_social")
    public String transportistaRazonSocial;

    @Column(name = "transportista_tipo_identificacion")
    public String transportistaTipoIdentificacion;

    @Column(name = "transportista_ruc")
    public String transportistaRuc;

    @Column(name = "dir_partida")
    public String dirPartida;

    @Column(name = "fecha_ini_transporte")
    public Instant fechaIniTransporte;

    @Column(name = "fecha_fin_transporte")
    public Instant fechaFinTransporte;

    @Column
    public String placa;

    /** Serializados como JSON — mismo patrón que {@code impuestos_json} en DetalleComprobanteEntity (ver ComprobanteMapper). */
    @Column(name = "pagos_json", columnDefinition = "text")
    public String pagosJson;

    @Column(name = "campos_adicionales_json", columnDefinition = "text")
    public String camposAdicionalesJson;

    @OneToMany(mappedBy = "comprobante", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id")
    public List<DestinatarioGuiaRemisionEntity> destinatarios = new ArrayList<>();

    @OneToMany(mappedBy = "comprobante", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id")
    public List<DetalleComprobanteEntity> detalles = new ArrayList<>();

    @Column(name = "creado_en", nullable = false)
    public Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    public Instant actualizadoEn;
}
