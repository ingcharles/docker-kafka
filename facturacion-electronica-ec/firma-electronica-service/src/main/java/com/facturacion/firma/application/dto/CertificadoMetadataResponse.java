package com.facturacion.firma.application.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Nunca incluye el contenido del .p12 ni su contraseña — solo metadata. Ver
 * {@code infrastructure.crypto.Cifrador} para cómo se protege lo demás.
 */
public class CertificadoMetadataResponse {

    private UUID id;
    private UUID empresaId;
    private String alias;
    private String huellaDigital;
    private Instant fechaEmisionCertificado;
    private Instant fechaExpiracionCertificado;
    private String estado;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(UUID empresaId) {
        this.empresaId = empresaId;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getHuellaDigital() {
        return huellaDigital;
    }

    public void setHuellaDigital(String huellaDigital) {
        this.huellaDigital = huellaDigital;
    }

    public Instant getFechaEmisionCertificado() {
        return fechaEmisionCertificado;
    }

    public void setFechaEmisionCertificado(Instant fechaEmisionCertificado) {
        this.fechaEmisionCertificado = fechaEmisionCertificado;
    }

    public Instant getFechaExpiracionCertificado() {
        return fechaExpiracionCertificado;
    }

    public void setFechaExpiracionCertificado(Instant fechaExpiracionCertificado) {
        this.fechaExpiracionCertificado = fechaExpiracionCertificado;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
