package com.facturacion.comprobantes.domain.model.enums;

/** Tabla 3 de la ficha técnica del SRI — código de documento embebido en la clave de acceso. */
public enum TipoComprobante {
    FACTURA("01"),
    LIQUIDACION_COMPRA("03"),
    NOTA_CREDITO("04"),
    NOTA_DEBITO("05"),
    GUIA_REMISION("06"),
    RETENCION("07");

    private final String codigo;

    TipoComprobante(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
