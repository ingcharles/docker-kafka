package com.facturacion.notificacion.shared.error;

import com.facturacion.commons.error.MensajeCatalogo;

/**
 * Logs de este servicio con su código estable y la clave de su texto en {@code mensajes.properties}. El primer dígito del
 * número es el nivel: 1 DEBUG, 2 INFO, 3 WARN, 4 ERROR, 5 FATAL. Se registran con {@code LogCodificado}; el catálogo
 * completo está en CATALOGO_ERRORES.md.
 */
public enum CodigoLog implements MensajeCatalogo {

    EVENTO_DUPLICADO_DESCARTADO("NOT-201", "log.evento_duplicado_descartado"),
    PLANTILLA_GUARDADA("NOT-202", "log.plantilla_guardada"),
    PLANTILLA_ELIMINADA("NOT-203", "log.plantilla_eliminada"),
    SIN_CORREO_RECEPTOR("NOT-301", "log.sin_correo_receptor"),
    PLANTILLA_EMPRESA_FALLIDA("NOT-302", "log.plantilla_empresa_fallida"),
    LOGO_NO_VALIDO("NOT-303", "log.logo_no_valido"),
    CORREO_FALLIDO("NOT-401", "log.correo_fallido"),
    OUTBOX_PUBLICACION_FALLIDA("NOT-402", "log.outbox_publicacion_fallida");

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
