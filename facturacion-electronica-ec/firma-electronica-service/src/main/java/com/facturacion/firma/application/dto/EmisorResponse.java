package com.facturacion.firma.application.dto;

import java.time.Instant;
import java.util.UUID;

public class EmisorResponse {

    private UUID id;
    private String ruc;
    private String razonSocial;
    private String nombreComercial;
    private String direccionMatriz;
    private boolean obligadoContabilidad;
    private boolean contribuyenteEspecial;
    private String numeroContribuyenteEspecial;
    private boolean esGranContribuyente;
    private String numeroResolucionGranContribuyente;
    private boolean vendeCombustibles;
    private String ambiente;
    private String establecimientoDefecto;
    private String puntoEmisionDefecto;
    private boolean activo;
    private Instant creadoEn;
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

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getNombreComercial() {
        return nombreComercial;
    }

    public void setNombreComercial(String nombreComercial) {
        this.nombreComercial = nombreComercial;
    }

    public String getDireccionMatriz() {
        return direccionMatriz;
    }

    public void setDireccionMatriz(String direccionMatriz) {
        this.direccionMatriz = direccionMatriz;
    }

    public boolean isObligadoContabilidad() {
        return obligadoContabilidad;
    }

    public void setObligadoContabilidad(boolean obligadoContabilidad) {
        this.obligadoContabilidad = obligadoContabilidad;
    }

    public boolean isContribuyenteEspecial() {
        return contribuyenteEspecial;
    }

    public void setContribuyenteEspecial(boolean contribuyenteEspecial) {
        this.contribuyenteEspecial = contribuyenteEspecial;
    }

    public String getNumeroContribuyenteEspecial() {
        return numeroContribuyenteEspecial;
    }

    public void setNumeroContribuyenteEspecial(String numeroContribuyenteEspecial) {
        this.numeroContribuyenteEspecial = numeroContribuyenteEspecial;
    }

    public boolean isEsGranContribuyente() {
        return esGranContribuyente;
    }

    public void setEsGranContribuyente(boolean esGranContribuyente) {
        this.esGranContribuyente = esGranContribuyente;
    }

    public String getNumeroResolucionGranContribuyente() {
        return numeroResolucionGranContribuyente;
    }

    public void setNumeroResolucionGranContribuyente(String numeroResolucionGranContribuyente) {
        this.numeroResolucionGranContribuyente = numeroResolucionGranContribuyente;
    }

    public boolean isVendeCombustibles() {
        return vendeCombustibles;
    }

    public void setVendeCombustibles(boolean vendeCombustibles) {
        this.vendeCombustibles = vendeCombustibles;
    }

    public String getAmbiente() {
        return ambiente;
    }

    public void setAmbiente(String ambiente) {
        this.ambiente = ambiente;
    }

    public String getEstablecimientoDefecto() {
        return establecimientoDefecto;
    }

    public void setEstablecimientoDefecto(String establecimientoDefecto) {
        this.establecimientoDefecto = establecimientoDefecto;
    }

    public String getPuntoEmisionDefecto() {
        return puntoEmisionDefecto;
    }

    public void setPuntoEmisionDefecto(String puntoEmisionDefecto) {
        this.puntoEmisionDefecto = puntoEmisionDefecto;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(Instant creadoEn) {
        this.creadoEn = creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    public void setActualizadoEn(Instant actualizadoEn) {
        this.actualizadoEn = actualizadoEn;
    }
}
