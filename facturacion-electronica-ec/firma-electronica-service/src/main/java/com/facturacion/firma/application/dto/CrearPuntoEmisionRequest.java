package com.facturacion.firma.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CrearPuntoEmisionRequest {

    @NotBlank
    @Pattern(regexp = "\\d{3}", message = "el código de punto de emisión debe tener 3 dígitos numéricos")
    private String codigo;

    private String descripcion;

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
