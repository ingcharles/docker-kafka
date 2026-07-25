package com.facturacion.sri.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "envio_sri")
public class EnvioSriEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "comprobante_id", nullable = false)
    public UUID comprobanteId;

    @Column(name = "clave_acceso", nullable = false, unique = true)
    public String claveAcceso;

    @Column(nullable = false)
    public String ambiente;

    @Column(name = "xml_firmado", nullable = false, columnDefinition = "text")
    public String xmlFirmado;

    @Column(nullable = false)
    public String estado;

    @Column(name = "mensajes_recepcion", columnDefinition = "text")
    public String mensajesRecepcion;

    @Column(name = "intentos_autorizacion", nullable = false)
    public int intentosAutorizacion;

    @Column(name = "ultima_consulta_en")
    public Instant ultimaConsultaEn;

    @Column(name = "xml_autorizado", columnDefinition = "text")
    public String xmlAutorizado;

    @Column(name = "numero_autorizacion")
    public String numeroAutorizacion;

    @Column(name = "fecha_autorizacion")
    public Instant fechaAutorizacion;

    @Column(name = "motivo_rechazo")
    public String motivoRechazo;

    @Column(name = "creado_en", nullable = false)
    public Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    public Instant actualizadoEn;
}
