package com.facturacion.comprobantes.domain.model.enums;

/**
 * Códigos de la Tabla 24 (formas de pago) que el código trata de forma especial. NO es el catálogo: la lista
 * completa de formas de pago y su vigencia viven en la tabla {@code forma_pago} (administrable por la API de catálogos); aquí
 * solo están los dos códigos sobre los que hay una regla.
 */
public enum FormaPagoSri {

    /** Se asume cuando el llamador no manda pagos. */
    SIN_UTILIZACION_SISTEMA_FINANCIERO("01"),

    /** Identifica una factura comercial negociable (Anexo 11 de la ficha técnica). */
    ENDOSO_TITULOS("21");

    private final String codigo;

    FormaPagoSri(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
