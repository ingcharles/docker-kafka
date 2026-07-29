package com.facturacion.identidad.domain.model.entity;

import java.time.Instant;
import java.util.UUID;

/**
 * Copia local de solo lectura de un establecimiento de firma-electronica-service
 * — se actualiza consumiendo
 * {@code facturacion.empresa.establecimientos.eventos.v1}. Se usa para
 * validar que el establecimiento/punto de emisión al que se quiere acotar
 * una api-key ({@code ApiKeyService.crear}) sea real y pertenezca al ruc
 * indicado. Indexada por (ruc, codigo), no por ningún id interno de
 * firma-electronica-service (mismo principio "Empresa sin ID").
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
