package com.facturacion.comprobantes.shared.error;

import com.facturacion.commons.error.MensajeCatalogo;

/**
 * Logs de este servicio con su código estable y la clave de su texto en {@code mensajes.properties}. El primer dígito del
 * número es el nivel: 1 DEBUG, 2 INFO, 3 WARN, 4 ERROR, 5 FATAL. Se registran con {@code LogCodificado}; el catálogo
 * completo está en CATALOGO_ERRORES.md.
 */
public enum CodigoLog implements MensajeCatalogo {

    COMANDO_DUPLICADO_DESCARTADO("COMP-201", "log.comando_duplicado_descartado"),
    COMANDO_APLICADO("COMP-202", "log.comando_aplicado"),
    EMISION_REPETIDA_DEVUELTA("COMP-203", "log.emision_repetida_devuelta"),
    IDEMPOTENCIA_PURGADA("COMP-204", "log.idempotencia_purgada"),
    COMANDO_IGNORADO("COMP-301", "log.comando_ignorado"),
    REENVIO_NO_APLICA("COMP-302", "log.reenvio_no_aplica"),
    LOGO_NO_VALIDO("COMP-303", "log.logo_no_valido"),
    COMANDO_FUERA_DE_ORDEN("COMP-401", "log.comando_fuera_de_orden"),
    OUTBOX_PUBLICACION_FALLIDA("COMP-402", "log.outbox_publicacion_fallida");

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
