package com.facturacion.firma.shared.error;

import com.facturacion.commons.error.MensajeCatalogo;

/**
 * Logs de este servicio con su código estable y la clave de su texto en {@code mensajes.properties}. El primer dígito del
 * número es el nivel: 1 DEBUG, 2 INFO, 3 WARN, 4 ERROR, 5 FATAL. Se registran con {@code LogCodificado}; el catálogo
 * completo está en CATALOGO_ERRORES.md.
 */
public enum CodigoLog implements MensajeCatalogo {

    COMANDO_DUPLICADO_DESCARTADO("FIR-201", "log.comando_duplicado_descartado"),
    CERTIFICADOS_VENCIDOS("FIR-301", "log.certificados_vencidos"),
    FIRMA_FALLIDA("FIR-401", "log.firma_fallida"),
    OUTBOX_PUBLICACION_FALLIDA("FIR-402", "log.outbox_publicacion_fallida"),
    OUTBOX_SIN_EMISOR("FIR-403", "log.outbox_sin_emisor");

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
