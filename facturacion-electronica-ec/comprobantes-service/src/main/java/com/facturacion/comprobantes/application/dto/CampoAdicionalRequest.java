package com.facturacion.comprobantes.application.dto;

import jakarta.validation.constraints.NotBlank;

/** Una línea de {@code <infoAdicional><campoAdicional>} — ver {@code CampoAdicional} en el dominio. */
public class CampoAdicionalRequest {

    @NotBlank
    private String nombre;

    @NotBlank
    private String valor;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }
}
