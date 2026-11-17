package com.facturacion.comprobantes.domain.model.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Copia local de solo lectura de un ítem de inventario-service — ver el
 * comentario en V3__item_local.sql. Se actualiza consumiendo
 * {@code facturacion.inventario.item.eventos.v1}; nunca se escribe desde
 * este servicio por ningún otro camino.
 */
public class ItemLocal {

    private UUID id;
    /** RUC de la empresa dueña de este ítem — el catálogo de inventario-service es multi-tenant, scoped por ruc (ver ARQUITECTURA.md). */
    private String ruc;
    private String codigoPrincipal;
    private String descripcion;
    /** Unidad de medida del ítem (p. ej. UN); sale en los detalles adicionales de la línea como "Medida". */
    private String unidadMedida;
    private String codigoImpuesto;
    private String codigoPorcentaje;
    private boolean tieneIce;
    private String codigoPorcentajeIce;
    private BigDecimal tarifaIce;
    private boolean activo;
    private Instant actualizadoEn;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public String getCodigoPrincipal() {
        return codigoPrincipal;
    }

    public void setCodigoPrincipal(String codigoPrincipal) {
        this.codigoPrincipal = codigoPrincipal;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getUnidadMedida() {
        return unidadMedida;
    }

    public void setUnidadMedida(String unidadMedida) {
        this.unidadMedida = unidadMedida;
    }

    public String getCodigoImpuesto() {
        return codigoImpuesto;
    }

    public void setCodigoImpuesto(String codigoImpuesto) {
        this.codigoImpuesto = codigoImpuesto;
    }

    public String getCodigoPorcentaje() {
        return codigoPorcentaje;
    }

    public void setCodigoPorcentaje(String codigoPorcentaje) {
        this.codigoPorcentaje = codigoPorcentaje;
    }

    public boolean isTieneIce() {
        return tieneIce;
    }

    public void setTieneIce(boolean tieneIce) {
        this.tieneIce = tieneIce;
    }

    public String getCodigoPorcentajeIce() {
        return codigoPorcentajeIce;
    }

    public void setCodigoPorcentajeIce(String codigoPorcentajeIce) {
        this.codigoPorcentajeIce = codigoPorcentajeIce;
    }

    public BigDecimal getTarifaIce() {
        return tarifaIce;
    }

    public void setTarifaIce(BigDecimal tarifaIce) {
        this.tarifaIce = tarifaIce;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    public void setActualizadoEn(Instant actualizadoEn) {
        this.actualizadoEn = actualizadoEn;
    }
}
