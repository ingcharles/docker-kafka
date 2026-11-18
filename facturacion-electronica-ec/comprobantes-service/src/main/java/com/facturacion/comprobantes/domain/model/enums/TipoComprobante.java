package com.facturacion.comprobantes.domain.model.enums;

import java.util.Arrays;
import java.util.Optional;

/** Tabla 3 de la ficha técnica del SRI — código de documento embebido en la clave de acceso, y su nombre impreso. */
public enum TipoComprobante {
    FACTURA("01", "FACTURA"),
    LIQUIDACION_COMPRA("03", "LIQUIDACIÓN DE COMPRA"),
    NOTA_CREDITO("04", "NOTA DE CRÉDITO"),
    NOTA_DEBITO("05", "NOTA DE DÉBITO"),
    GUIA_REMISION("06", "GUÍA DE REMISIÓN"),
    RETENCION("07", "COMPROBANTE DE RETENCIÓN");

    private final String codigo;
    private final String nombre;

    TipoComprobante(String codigo, String nombre) {
        this.codigo = codigo;
        this.nombre = nombre;
    }

    public String codigo() {
        return codigo;
    }

    /** Nombre del documento como se imprime en el RIDE y el recibo. */
    public String nombre() {
        return nombre;
    }

    public static Optional<TipoComprobante> buscarPorCodigo(String codigo) {
        return Arrays.stream(values()).filter(t -> t.codigo.equals(codigo)).findFirst();
    }
}
