package com.facturacion.comprobantes.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Datos del emisor enviados inline en cada request de creación de
 * comprobante — este servicio ya no mantiene una copia local de Emisor ni
 * depende de un {@code emisorId} de firma-electronica-service (ver
 * ARQUITECTURA.md, "Emisor sin ID"). El único dato que firma-electronica-service
 * necesita para firmar es {@link #ruc}, que viaja tal cual dentro del
 * comando {@code FirmarComprobanteCommand} — el certificado se sigue
 * gestionando y guardando por separado ahí, no acá ni en este request.
 */
public class EmisorRequest {

    @NotBlank
    @Pattern(regexp = "\\d{13}", message = "el RUC debe tener 13 dígitos")
    private String ruc;

    @NotBlank
    private String razonSocial;

    private String nombreComercial;

    @NotBlank
    private String direccionMatriz;

    /** Dirección del establecimiento emisor — si no se manda, se usa direccionMatriz (mismo criterio que antes con EmisorLocal). */
    private String direccionEstablecimiento;

    private boolean obligadoContabilidad = false;

    private boolean contribuyenteEspecial = false;

    private String numeroContribuyenteEspecial;

    private boolean esGranContribuyente = false;

    private String numeroResolucionGranContribuyente;

    /** PRUEBAS o PRODUCCION — Tabla 4 de la ficha técnica del SRI. */
    @NotBlank
    private String ambiente;

    /** Código de establecimiento (estab), ej. "001". */
    @NotBlank
    private String establecimiento;

    /** Código de punto de emisión (ptoEmi), ej. "001". */
    @NotBlank
    private String puntoEmision;

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

    public String getDireccionEstablecimiento() {
        return direccionEstablecimiento;
    }

    public void setDireccionEstablecimiento(String direccionEstablecimiento) {
        this.direccionEstablecimiento = direccionEstablecimiento;
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

    public String getAmbiente() {
        return ambiente;
    }

    public void setAmbiente(String ambiente) {
        this.ambiente = ambiente;
    }

    public String getEstablecimiento() {
        return establecimiento;
    }

    public void setEstablecimiento(String establecimiento) {
        this.establecimiento = establecimiento;
    }

    public String getPuntoEmision() {
        return puntoEmision;
    }

    public void setPuntoEmision(String puntoEmision) {
        this.puntoEmision = puntoEmision;
    }
}
