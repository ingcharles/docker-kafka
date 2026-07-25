package com.facturacion.firma.domain.model.entity;

import java.time.Instant;
import java.util.UUID;

import com.facturacion.firma.domain.model.enums.EstadoCertificado;

/**
 * Certificado de firma electrónica (.p12) de un emisor. El contenido y la
 * contraseña viajan siempre cifrados — ver {@code infrastructure.crypto.CifradoService}.
 * Un emisor puede acumular varios certificados en el tiempo (renovaciones),
 * pero solo uno puede estar {@link EstadoCertificado#ACTIVO} a la vez.
 */
public class CertificadoDigital {

    private UUID id;
    private UUID emisorId;
    private String alias;
    private String huellaDigital;
    private byte[] contenidoCifrado;
    private byte[] contrasenaCifrada;
    private Instant fechaEmisionCertificado;
    private Instant fechaExpiracionCertificado;
    private EstadoCertificado estado;
    private Instant creadoEn;

    public CertificadoDigital() {
    }

    public CertificadoDigital(UUID id, UUID emisorId, String alias, String huellaDigital, byte[] contenidoCifrado,
            byte[] contrasenaCifrada, Instant fechaEmisionCertificado, Instant fechaExpiracionCertificado,
            EstadoCertificado estado, Instant creadoEn) {
        this.id = id;
        this.emisorId = emisorId;
        this.alias = alias;
        this.huellaDigital = huellaDigital;
        this.contenidoCifrado = contenidoCifrado;
        this.contrasenaCifrada = contrasenaCifrada;
        this.fechaEmisionCertificado = fechaEmisionCertificado;
        this.fechaExpiracionCertificado = fechaExpiracionCertificado;
        this.estado = estado;
        this.creadoEn = creadoEn;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getEmisorId() {
        return emisorId;
    }

    public void setEmisorId(UUID emisorId) {
        this.emisorId = emisorId;
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

    public byte[] getContenidoCifrado() {
        return contenidoCifrado;
    }

    public void setContenidoCifrado(byte[] contenidoCifrado) {
        this.contenidoCifrado = contenidoCifrado;
    }

    public byte[] getContrasenaCifrada() {
        return contrasenaCifrada;
    }

    public void setContrasenaCifrada(byte[] contrasenaCifrada) {
        this.contrasenaCifrada = contrasenaCifrada;
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

    public EstadoCertificado getEstado() {
        return estado;
    }

    public void setEstado(EstadoCertificado estado) {
        this.estado = estado;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(Instant creadoEn) {
        this.creadoEn = creadoEn;
    }
}
