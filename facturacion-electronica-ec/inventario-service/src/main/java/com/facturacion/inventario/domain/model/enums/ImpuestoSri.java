package com.facturacion.inventario.domain.model.enums;

/** Códigos de impuesto de la Tabla 16 de la ficha técnica del SRI que tienen tratamiento propio en el código. */
public enum ImpuestoSri {

    IVA("2"),
    ICE("3"),
    IRBPNR("5");

    private final String codigo;

    ImpuestoSri(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
