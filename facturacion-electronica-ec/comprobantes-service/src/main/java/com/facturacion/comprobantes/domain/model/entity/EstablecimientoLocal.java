package com.facturacion.comprobantes.domain.model.entity;

import java.time.Instant;
import java.util.UUID;

/**
 * Copia local de solo lectura de un establecimiento de firma-electronica-service
 * — se actualiza consumiendo
 * {@code facturacion.empresa.establecimientos.eventos.v1}. A diferencia de
 * {@link ItemLocal} (indexado por el id real del ítem, porque
 * {@code DetalleComprobanteRequest.itemId} lo referencia directamente), esta
 * tabla se indexa por la clave natural (ruc, codigo) — comprobantes-service
 * nunca conoce el UUID interno de firma-electronica-service para un
 * establecimiento (mismo principio "Empresa sin ID" de ARQUITECTURA.md).
 */
public class EstablecimientoLocal {

    private UUID id;
    private String ruc;
    private String codigo;
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

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
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
