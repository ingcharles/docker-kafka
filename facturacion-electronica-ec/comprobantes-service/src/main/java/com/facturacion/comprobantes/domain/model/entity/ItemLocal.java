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
    private String codigoPrincipal;
    private String descripcion;
    private String codigoImpuesto;
    private String codigoPorcentaje;
    private BigDecimal tarifaIva;
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

    public BigDecimal getTarifaIva() {
        return tarifaIva;
    }

    public void setTarifaIva(BigDecimal tarifaIva) {
        this.tarifaIva = tarifaIva;
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
