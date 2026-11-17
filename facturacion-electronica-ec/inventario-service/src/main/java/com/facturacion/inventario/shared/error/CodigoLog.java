package com.facturacion.inventario.shared.error;

import com.facturacion.commons.error.MensajeCatalogo;

/**
 * Logs de este servicio con su código estable y la clave de su texto en {@code mensajes.properties}. El primer dígito del
 * número es el nivel: 1 DEBUG, 2 INFO, 3 WARN, 4 ERROR, 5 FATAL. Se registran con {@code LogCodificado}; el catálogo
 * completo está en CATALOGO_ERRORES.md.
 */
public enum CodigoLog implements MensajeCatalogo {

    COMANDO_DUPLICADO_DESCARTADO("INV-201", "log.comando_duplicado_descartado"),
    STOCK_NO_DISPONIBLE("INV-301", "log.stock_no_disponible"),
    COMANDO_STOCK_DESCONOCIDO("INV-302", "log.comando_stock_desconocido"),
    OUTBOX_PUBLICACION_FALLIDA("INV-401", "log.outbox_publicacion_fallida");

    private final String codigo;
    private final String claveMensaje;

    CodigoLog(String codigo, String claveMensaje) {
        this.codigo = codigo;
        this.claveMensaje = claveMensaje;
    }

    @Override
    public String codigo() {
        return codigo;
    }

    @Override
    public String claveMensaje() {
        return claveMensaje;
    }
}
