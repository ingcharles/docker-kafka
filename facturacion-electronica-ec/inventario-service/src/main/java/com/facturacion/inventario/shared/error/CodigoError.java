package com.facturacion.inventario.shared.error;

import com.facturacion.commons.error.ErrorCatalogo;

/**
 * Catálogo de errores HTTP de inventario-service (prefijo {@code INV}). Cada uno lleva el {@code codigo} estable que
 * recibe el cliente, el {@code estadoHttp} y la {@code claveMensaje} del texto en {@code mensajes.properties}
 * (con {@code {0}}, {@code {1}}... para los parámetros). El texto nunca se escribe en el código.
 */
public enum CodigoError implements ErrorCatalogo {

    // Clasificación del ítem
    CATEGORIA_INCOMPATIBLE("INV-400-001", 400, "item.categoria_incompatible"),
    ITEM_NO_ES_PRODUCTO("INV-400-002", 400, "item.no_es_producto"),
    ICE_SIN_CODIGO_PORCENTAJE("INV-400-003", 400, "item.ice.sin_codigo_porcentaje"),
    ICE_SIN_TARIFA("INV-400-004", 400, "item.ice.sin_tarifa"),

    // Imágenes
    IMAGEN_INVALIDA("INV-400-005", 400, "imagen.invalida"),

    // Existencias
    STOCK_INSUFICIENTE("INV-409-001", 409, "stock.insuficiente");

    private final String codigo;
    private final int estadoHttp;
    private final String claveMensaje;

    CodigoError(String codigo, int estadoHttp, String claveMensaje) {
        this.codigo = codigo;
        this.estadoHttp = estadoHttp;
        this.claveMensaje = claveMensaje;
    }

    @Override
    public String codigo() {
        return codigo;
    }

    @Override
    public int estadoHttp() {
        return estadoHttp;
    }

    @Override
    public String claveMensaje() {
        return claveMensaje;
    }
}
