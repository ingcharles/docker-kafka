package com.facturacion.firma.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class CrearEmpresaRequest {

    @NotBlank
    @Pattern(regexp = "\\d{13}", message = "el RUC debe tener 13 dígitos numéricos")
    private String ruc;

    @NotBlank
    private String razonSocial;

    private String nombreComercial;

    @NotBlank
    private String direccionMatriz;

    private boolean obligadoContabilidad;

    private boolean contribuyenteEspecial;

    private String numeroContribuyenteEspecial;

    private boolean esGranContribuyente;

    private String numeroResolucionGranContribuyente;

    private boolean vendeCombustibles;

    @NotNull
    private String ambiente;

    private String establecimientoDefecto = "001";

    private String puntoEmisionDefecto = "001";

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
}
