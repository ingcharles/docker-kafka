package com.facturacion.comprobantes.domain.model.entity;

import java.time.Instant;
import java.util.UUID;

/**
 * Copia local de solo lectura de un ajuste de {@code configuracion_empresa}
 * de firma-electronica-service — se actualiza consumiendo
 * {@code facturacion.empresa.configuracion.eventos.v1}. Genérica a
 * propósito (mismo store clave/valor que el original); hoy el único
 * consumidor real de una clave es {@code RideGenerador} para
 * {@code ride.color}. Indexada por (ruc, clave), no por ningún id interno
 * de firma-electronica-service (mismo principio "Empresa sin ID").
 */
public class ConfiguracionEmpresaLocal {

    private UUID id;
    private String ruc;
    private String clave;
    private String valor;
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

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    public void setActualizadoEn(Instant actualizadoEn) {
        this.actualizadoEn = actualizadoEn;
    }
}
