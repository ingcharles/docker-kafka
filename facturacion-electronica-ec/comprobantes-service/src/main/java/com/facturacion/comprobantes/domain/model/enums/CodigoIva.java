package com.facturacion.comprobantes.domain.model.enums;

/**
 * Códigos de porcentaje de IVA (Tabla 17) que no son una tarifa sino una <b>clasificación</b> del bien: no objeto de
 * impuesto y exento. Estructurales (el RIDE y las bases del XML los tratan aparte), por eso son enum y no catálogo;
 * las tarifas con valor (0 %, 5 %, 15 %, ...) sí van en {@code tarifa_impuesto}.
 */
public enum CodigoIva {

    NO_OBJETO("6"),
    EXENTO("7");

    private final String codigo;

    CodigoIva(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    public boolean esCodigo(String codigoPorcentaje) {
        return codigo.equals(codigoPorcentaje);
    }
}
