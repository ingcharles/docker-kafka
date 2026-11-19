package com.facturacion.notificacion.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "plantilla_correo")
public class PlantillaCorreoEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "empresa_ruc", nullable = false)
    public String empresaRuc;

    @Column(name = "tipo_comprobante", nullable = false)
    public String tipoComprobante;

    @Column(nullable = false)
    public String asunto;

    @Column(name = "cuerpo_html", nullable = false, columnDefinition = "text")
    public String cuerpoHtml;

    @Column(name = "cuerpo_texto", columnDefinition = "text")
    public String cuerpoTexto;

    @Column(nullable = false)
    public int version;

    @Column(nullable = false)
    public boolean activa;

    @Column(name = "creado_en", nullable = false)
    public Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    public Instant actualizadoEn;
}
