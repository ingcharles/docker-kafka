package com.facturacion.comprobantes.infrastructure.persistence.entity;

import java.time.Instant;
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
@Table(name = "destinatario_guia_remision")
public class DestinatarioGuiaRemisionEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comprobante_id", nullable = false)
    public ComprobanteEntity comprobante;

    @Column(nullable = false)
    public String identificacion;

    @Column(name = "razon_social", nullable = false)
    public String razonSocial;

    @Column(nullable = false)
    public String direccion;

    @Column(name = "motivo_traslado", nullable = false)
    public String motivoTraslado;

    @Column(name = "doc_aduanero_unico")
    public String docAduaneroUnico;

    @Column(name = "cod_estab_destino")
    public String codEstabDestino;

    @Column
    public String ruta;

    @Column(name = "cod_doc_sustento")
    public String codDocSustento;

    @Column(name = "num_doc_sustento")
    public String numDocSustento;

    @Column(name = "num_aut_doc_sustento")
    public String numAutDocSustento;

    @Column(name = "fecha_emision_doc_sustento")
    public Instant fechaEmisionDocSustento;
}
