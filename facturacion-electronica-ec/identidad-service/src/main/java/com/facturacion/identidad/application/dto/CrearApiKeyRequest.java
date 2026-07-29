package com.facturacion.identidad.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class CrearApiKeyRequest {

    @NotBlank
    private String nombre;

    @NotBlank
    @Pattern(regexp = "\\d{13}", message = "el RUC debe tener 13 dígitos")
    private String ruc;

    /** 0 = sin expiración. Tope real aplicado en el servicio, ver identidad.jwt.apikey.max-dias. */
    @NotNull
    private Integer expiracionEnDias = 365;

    /**
     * Opcional — acota la key a un solo establecimiento (empresas con varias
     * sucursales/cajas que no quieren que una key de un punto de venta pueda
     * emitir con el de otro). {@code null} (por defecto) = sin restricción,
     * mismo comportamiento que antes de que este campo existiera.
     */
    @Pattern(regexp = "\\d{3}", message = "el código de establecimiento debe tener 3 dígitos numéricos")
    private String establecimiento;

    /** Opcional — solo válido junto con {@code establecimiento}; acota además a un punto de emisión específico dentro de él. */
    @Pattern(regexp = "\\d{3}", message = "el código de punto de emisión debe tener 3 dígitos numéricos")
    private String puntoEmision;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public Integer getExpiracionEnDias() {
        return expiracionEnDias;
    }

    public void setExpiracionEnDias(Integer expiracionEnDias) {
        this.expiracionEnDias = expiracionEnDias;
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
