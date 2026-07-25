package com.facturacion.comprobantes.domain.model.enums;

/** Tabla 4 de la ficha técnica del SRI. Duplicado a propósito de firma-electronica-service — ver ARQUITECTURA.md, cero librerías compartidas entre servicios. */
public enum AmbienteSri {

    PRUEBAS("1"),
    PRODUCCION("2");

    private final String codigo;

    AmbienteSri(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
