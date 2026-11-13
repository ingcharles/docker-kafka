package com.facturacion.orquestacion.shared.error;

import com.facturacion.commons.error.MensajeCatalogo;

/**
 * Motivos que el orquestador registra en la saga (campo {@code failureReason} y bitácora {@code saga_step}).
 * Mismo esquema que en los demás servicios: {@code codigo} estable + {@code claveMensaje} del texto en
 * {@code mensajes.properties}. Aquí no hay respuestas HTTP de error, así que no llevan estado HTTP.
 */
public enum CodigoError implements MensajeCatalogo {

    SAGA_REINTENTO_AUTOMATICO("ORQ-001", "saga.reintento_automatico"),
    SAGA_EXPIRADA("ORQ-002", "saga.expirada"),
    SAGA_REINTENTO_FALLIDO("ORQ-003", "saga.reintento_fallido");

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
