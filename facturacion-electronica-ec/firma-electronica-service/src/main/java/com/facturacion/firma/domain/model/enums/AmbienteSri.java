package com.facturacion.firma.domain.model.enums;

/**
 * Tabla 4 de la ficha técnica del SRI: código de ambiente incrustado en la
 * clave de acceso y en el tag {@code <ambiente>} de todo comprobante.
 */
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
