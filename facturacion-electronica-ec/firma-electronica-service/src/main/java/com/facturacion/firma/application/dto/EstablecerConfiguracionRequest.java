package com.facturacion.firma.application.dto;

import jakarta.validation.constraints.NotBlank;

public class EstablecerConfiguracionRequest {

    @NotBlank
    private String valor;

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }
}
