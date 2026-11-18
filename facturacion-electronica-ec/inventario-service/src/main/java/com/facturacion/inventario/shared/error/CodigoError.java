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

    // Imágenes
    IMAGEN_VACIA("INV-400-005", 400, "imagen.vacia"),
    IMAGEN_MUY_GRANDE("INV-400-007", 400, "imagen.muy_grande"),
    IMAGEN_TIPO_NO_ADMITIDO("INV-400-008", 400, "imagen.tipo_no_admitido"),

    // Existencias
    ITEM_NO_ENCONTRADO("INV-404-001", 404, "item.no_encontrado"),
    CATEGORIA_NO_ENCONTRADA("INV-404-002", 404, "categoria.no_encontrada"),
    IMAGEN_NO_ENCONTRADA("INV-404-003", 404, "imagen.no_encontrada"),
    STOCK_INSUFICIENTE("INV-409-001", 409, "stock.insuficiente"),
    ARCHIVO_IMAGEN_ILEGIBLE("INV-400-006", 400, "imagen.archivo_ilegible");

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
