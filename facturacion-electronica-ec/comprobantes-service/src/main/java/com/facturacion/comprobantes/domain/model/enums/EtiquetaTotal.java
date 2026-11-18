package com.facturacion.comprobantes.domain.model.enums;

/** Las filas del resumen de totales impreso (RIDE y ticket, Tabla 21 de la ficha técnica) y su rótulo. */
public enum EtiquetaTotal {

    /** Subtotal de una tarifa de IVA; el rótulo lleva la tarifa ("SUBTOTAL 15%"). */
    SUBTOTAL_IVA("SUBTOTAL"),
    SUBTOTAL_NO_OBJETO_IVA("SUBTOTAL NO OBJETO DE IVA"),
    SUBTOTAL_EXENTO_IVA("SUBTOTAL EXENTO DE IVA"),
    SUBTOTAL_SIN_IMPUESTOS("SUBTOTAL SIN IMPUESTOS"),
    DESCUENTO("DESCUENTO"),
    ICE("ICE"),
    IRBPNR("IRBPNR"),
    /** IVA de una tarifa; el rótulo lleva la tarifa ("IVA 15%"). */
    IVA("IVA"),
    PROPINA("PROPINA"),
    VALOR_TOTAL("VALOR TOTAL");

    private final String rotulo;

    EtiquetaTotal(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
