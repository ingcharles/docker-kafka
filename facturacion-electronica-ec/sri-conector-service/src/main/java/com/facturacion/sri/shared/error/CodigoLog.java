package com.facturacion.sri.shared.error;

import com.facturacion.commons.error.MensajeCatalogo;

/**
 * Logs de este servicio con su código estable y la clave de su texto en {@code mensajes.properties}. El primer dígito del
 * número es el nivel: 1 DEBUG, 2 INFO, 3 WARN, 4 ERROR, 5 FATAL. Se registran con {@code LogCodificado}; el catálogo
 * completo está en CATALOGO_ERRORES.md.
 */
public enum CodigoLog implements MensajeCatalogo {

    RECEPCION_RESULTADO("SRI-201", "log.recepcion_resultado"),
    AUTORIZACION_PENDIENTE("SRI-202", "log.autorizacion_pendiente"),
    AUTORIZACION_RESULTADO("SRI-203", "log.autorizacion_resultado"),
    COMANDO_DUPLICADO_DESCARTADO("SRI-204", "log.comando_duplicado_descartado"),
    HTTP_AUTORIZACION("SRI-205", "log.http_autorizacion"),
    HTTP_RECEPCION("SRI-206", "log.http_recepcion"),
    URL_SRI_IGNORADA("SRI-301", "log.url_sri_ignorada"),
    SRI_RECHAZA_RECEPCION("SRI-401", "log.sri_rechaza_recepcion"),
    SRI_NO_AUTORIZA("SRI-402", "log.sri_no_autoriza"),
    SRI_SIN_RESPUESTA_FINAL("SRI-403", "log.sri_sin_respuesta_final"),
    CONSULTA_AUTORIZACION_FALLIDA("SRI-404", "log.consulta_autorizacion_fallida"),
    OUTBOX_PUBLICACION_FALLIDA("SRI-405", "log.outbox_publicacion_fallida");

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
