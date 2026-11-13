package com.facturacion.comprobantes.infrastructure.persistence.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

/** Tabla 20 (códigos y porcentajes de retención). Solo lectura: {@code PUT /catalogos/retencion}. */
@Entity
@Immutable
@Table(name = "retencion")
@IdClass(RetencionEntity.Clave.class)
public class RetencionEntity extends PanacheEntityBase {

    @Id
    @Column(name = "impuesto_codigo")
    public String impuestoCodigo;

    @Id
    public String codigo;

    @Id
    @Column(name = "vigente_desde")
    public LocalDate vigenteDesde;

    public String descripcion;

    public BigDecimal porcentaje;

    @Column(name = "vigente_hasta")
    public LocalDate vigenteHasta;

    public boolean activo;

    public static class Clave implements Serializable {
        public String impuestoCodigo;
        public String codigo;
        public LocalDate vigenteDesde;

        public Clave() {
        }

        @Override
        public boolean equals(Object otro) {
            return otro instanceof Clave c && Objects.equals(impuestoCodigo, c.impuestoCodigo)
                && Objects.equals(codigo, c.codigo) && Objects.equals(vigenteDesde, c.vigenteDesde);
        }

        @Override
        public int hashCode() {
            return Objects.hash(impuestoCodigo, codigo, vigenteDesde);
        }
    }
}
