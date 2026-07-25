package com.facturacion.notificacion.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "notificacion_enviada")
public class NotificacionEnviadaEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "comprobante_id", nullable = false)
    public UUID comprobanteId;

    @Column(name = "clave_acceso", nullable = false)
    public String claveAcceso;

    @Column(nullable = false)
    public String destinatario;

    @Column(nullable = false)
    public String estado;

    @Column(name = "motivo_error")
    public String motivoError;

    @Column(name = "enviado_en", nullable = false)
    public Instant enviadoEn;
}
