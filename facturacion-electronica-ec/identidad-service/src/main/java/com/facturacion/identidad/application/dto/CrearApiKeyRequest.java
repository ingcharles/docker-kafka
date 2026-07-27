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
}
