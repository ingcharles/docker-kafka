package com.facturacion.comprobantes.domain.model.enums;

/**
 * Tipos de impuesto de la Tabla 16 de la ficha técnica del SRI. Son los que el código trata cada uno con su propia
 * regla de cálculo; las <b>tarifas</b> (IVA 15 %, ICE 75 %, IRBPNR 0,02, ...) no se listan aquí: cambian con la
 * normativa y viven en el catálogo {@code tarifa_impuesto}.
 */
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

    /** ¿Este código del SRI es el de este impuesto? (tolera {@code null}: un código ausente no coincide con ninguno). */
    public boolean esCodigo(String codigoSri) {
        return codigo.equals(codigoSri);
    }
}
