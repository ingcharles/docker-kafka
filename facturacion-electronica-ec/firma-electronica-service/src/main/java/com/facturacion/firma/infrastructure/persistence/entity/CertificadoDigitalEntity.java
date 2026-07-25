package com.facturacion.firma.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "certificado_digital")
public class CertificadoDigitalEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "emisor_id", nullable = false)
    public UUID emisorId;

    @Column
    public String alias;

    @Column(name = "huella_digital", nullable = false)
    public String huellaDigital;

    @Column(name = "contenido_cifrado", nullable = false)
    public byte[] contenidoCifrado;

    @Column(name = "contrasena_cifrada", nullable = false)
    public byte[] contrasenaCifrada;

    @Column(name = "fecha_emision_certificado", nullable = false)
    public Instant fechaEmisionCertificado;

    @Column(name = "fecha_expiracion_certificado", nullable = false)
    public Instant fechaExpiracionCertificado;

    @Column(nullable = false)
    public String estado;

    @Column(name = "creado_en", nullable = false)
    public Instant creadoEn;
}
