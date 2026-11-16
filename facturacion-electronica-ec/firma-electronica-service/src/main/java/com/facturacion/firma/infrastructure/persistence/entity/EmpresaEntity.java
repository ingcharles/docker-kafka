package com.facturacion.firma.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "empresa")
public class EmpresaEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false, unique = true)
    public String ruc;

    @Column(name = "razon_social", nullable = false)
    public String razonSocial;

    @Column(name = "nombre_comercial")
    public String nombreComercial;

    @Column(name = "direccion_matriz", nullable = false)
    public String direccionMatriz;

    @Column(name = "obligado_contabilidad", nullable = false)
    public boolean obligadoContabilidad;

    @Column(name = "contribuyente_especial", nullable = false)
    public boolean contribuyenteEspecial;

    @Column(name = "numero_contribuyente_especial")
    public String numeroContribuyenteEspecial;

    @Column(name = "es_gran_contribuyente", nullable = false)
    public boolean esGranContribuyente;

    @Column(name = "numero_resolucion_gran_contribuyente")
    public String numeroResolucionGranContribuyente;

    @Column(name = "vende_combustibles", nullable = false)
    public boolean vendeCombustibles;

    @Column(nullable = false)
    public String ambiente;

    @Column(name = "establecimiento_defecto", nullable = false)
    public String establecimientoDefecto;

    @Column(name = "punto_emision_defecto", nullable = false)
    public String puntoEmisionDefecto;

    @Column(nullable = false)
    public boolean activo;

    @Column(name = "creado_en", nullable = false)
    public Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    public Instant actualizadoEn;

    @Column(name = "logo")
    public byte[] logo;

    @Column(name = "logo_tipo")
    public String logoTipo;
}
