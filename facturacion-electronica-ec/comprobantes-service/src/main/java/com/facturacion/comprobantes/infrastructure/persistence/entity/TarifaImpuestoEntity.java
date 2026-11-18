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

/**
 * Tabla 17 (porcentajes de IVA y demás tarifas). De aquí sale la tarifa de cada línea, así que cambiar el IVA es
 * cambiar una fila ({@code PUT /catalogos/tarifa_impuesto}), sin tocar ítems ni código. Solo lectura.
 */
@Entity
@Immutable
@Table(name = "tarifa_impuesto")
@IdClass(TarifaImpuestoEntity.Clave.class)
public class TarifaImpuestoEntity extends PanacheEntityBase {

    @Id
    @Column(name = "impuesto_codigo")
    public String impuestoCodigo;

    @Id
    @Column(name = "codigo_porcentaje")
    public String codigoPorcentaje;

    public String descripcion;

    public BigDecimal tarifa;

    @Id
    @Column(name = "tipo_tarifa")
    public String tipoTarifa;

    @Id
    @Column(name = "vigente_desde")
    public LocalDate vigenteDesde;

    @Column(name = "vigente_hasta")
    public LocalDate vigenteHasta;

    public boolean activo;

    public static class Clave implements Serializable {
        public String impuestoCodigo;
        public String codigoPorcentaje;
        public String tipoTarifa;
        public LocalDate vigenteDesde;

        public Clave() {
        }

        @Override
        public boolean equals(Object otro) {
            return otro instanceof Clave c && Objects.equals(impuestoCodigo, c.impuestoCodigo)
                && Objects.equals(codigoPorcentaje, c.codigoPorcentaje)
                && Objects.equals(tipoTarifa, c.tipoTarifa) && Objects.equals(vigenteDesde, c.vigenteDesde);
        }

        @Override
        public int hashCode() {
            return Objects.hash(impuestoCodigo, codigoPorcentaje, tipoTarifa, vigenteDesde);
        }
    }
}
