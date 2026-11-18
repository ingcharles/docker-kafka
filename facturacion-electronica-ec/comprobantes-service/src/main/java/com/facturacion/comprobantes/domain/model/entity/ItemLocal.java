package com.facturacion.comprobantes.domain.model.entity;

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
    /** Código de porcentaje de IVA del ítem (Tabla 17); la tarifa sale del catálogo al emitir. */
    private String codigoPorcentajeIva;
    /** Código de ICE (Tabla 18) o {@code null} si el ítem no lleva ICE. */
    private String codigoIce;
    /** Código de IRBPNR (5001) o {@code null} si el ítem no lo lleva. */
    private String codigoIrbpnr;
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

    public String getCodigoPorcentajeIva() {
        return codigoPorcentajeIva;
    }

    public void setCodigoPorcentajeIva(String codigoPorcentajeIva) {
        this.codigoPorcentajeIva = codigoPorcentajeIva;
    }

    public String getCodigoIce() {
        return codigoIce;
    }

    public void setCodigoIce(String codigoIce) {
        this.codigoIce = codigoIce;
    }

    public String getCodigoIrbpnr() {
        return codigoIrbpnr;
    }

    public void setCodigoIrbpnr(String codigoIrbpnr) {
        this.codigoIrbpnr = codigoIrbpnr;
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
