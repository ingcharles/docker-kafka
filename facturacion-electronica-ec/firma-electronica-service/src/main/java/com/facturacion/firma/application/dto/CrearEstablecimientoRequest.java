package com.facturacion.firma.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CrearEstablecimientoRequest {

    @NotBlank
    @Pattern(regexp = "\\d{3}", message = "el código de establecimiento debe tener 3 dígitos numéricos")
    private String codigo;

    @NotBlank
    private String direccion;

    @NotBlank
    @Pattern(regexp = "MATRIZ|SUCURSAL", message = "tipo debe ser MATRIZ o SUCURSAL")
    private String tipo;

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
}
