package com.facturacion.comprobantes.infrastructure.persistence.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "secuencial_comprobante")
public class SecuencialComprobanteEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "emisor_id", nullable = false)
    public UUID emisorId;

    @Column(name = "tipo_comprobante", nullable = false)
    public String tipoComprobante;

    @Column(nullable = false)
    public String establecimiento;

    @Column(name = "punto_emision", nullable = false)
    public String puntoEmision;

    @Column(name = "ultimo_secuencial", nullable = false)
    public long ultimoSecuencial;
}
