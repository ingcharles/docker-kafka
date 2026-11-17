package com.facturacion.orquestacion.shared.error;

import com.facturacion.commons.error.MensajeCatalogo;

/**
 * Motivos que el orquestador registra en la saga (campo {@code failureReason} y bitácora {@code saga_step}) y logs del servicio. El primer dígito del número es el nivel: 2 INFO, 4 ERROR.
 * Mismo esquema que en los demás servicios: {@code codigo} estable + {@code claveMensaje} del texto en
 * {@code mensajes.properties}. Aquí no hay respuestas HTTP de error, así que no llevan estado HTTP.
 */
public enum CodigoError implements MensajeCatalogo {

    SAGA_REINTENTO_AUTOMATICO("ORQ-201", "saga.reintento_automatico"),
    WATCHDOG_SAGAS_RECUPERADAS("ORQ-202", "log.watchdog_sagas_recuperadas"),
    SAGA_EXPIRADA("ORQ-401", "saga.expirada"),
    SAGA_REINTENTO_FALLIDO("ORQ-402", "saga.reintento_fallido"),
    OUTBOX_PUBLICACION_FALLIDA("ORQ-403", "log.outbox_publicacion_fallida"),
    OUTBOX_SIN_EMISOR("ORQ-404", "log.outbox_sin_emisor"),
    WATCHDOG_REVISION_FALLIDA("ORQ-405", "log.watchdog_revision_fallida"),
    REINTENTO_AUTOMATICO_FALLIDO("ORQ-406", "log.reintento_automatico_fallido"),
    REINTENTO_NO_REGISTRADO("ORQ-407", "log.reintento_no_registrado");

    private final String codigo;
    private final String claveMensaje;

    CodigoError(String codigo, String claveMensaje) {
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
