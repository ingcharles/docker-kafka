package com.facturacion.comprobantes.domain.model.enums;

/** Tabla 2 de la ficha técnica del SRI — tipo de emisión (último dígito antes del verificador de la clave de acceso). */
public enum TipoEmision {

    /** Emisión normal: único tipo del esquema offline que usa este servicio. */
    NORMAL("1");

    private final String codigo;

    TipoEmision(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
