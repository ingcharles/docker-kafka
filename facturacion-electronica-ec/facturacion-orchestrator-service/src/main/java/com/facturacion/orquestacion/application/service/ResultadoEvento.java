package com.facturacion.orquestacion.application.service;

import java.util.UUID;

import org.jboss.logging.Logger;

/** Qué hizo el orquestador con un evento recibido, para que quede registrado y las pruebas lo puedan verificar. */
public enum ResultadoEvento {

    /** La saga avanzó y se emitió lo que correspondía. */
    APLICADO,

    /** El evento ya se había procesado antes (inbox): no se hizo nada. */
    DUPLICADO,

    /** El tipo de evento no es uno de los que mueven la saga (p. ej. un evento informativo). */
    IGNORADO_TIPO,

    /**
     * La saga no estaba en un estado desde el que este evento tenga sentido (llegó tarde, fuera de orden o la saga ya
     * terminó): no se tocó. Es el caso que antes pasaba en silencio y por eso se registra como advertencia.
     */
    IGNORADO_ESTADO_NO_ESPERADO;

    /** Una línea de log por evento: INFO si todo es normal, WARN si se ignoró por el estado de la saga. */
    public void registrar(Logger log, String origen, UUID eventId, UUID comprobanteId) {
        if (this == IGNORADO_ESTADO_NO_ESPERADO) {
            log.warnf("Evento %s (%s) del comprobante %s ignorado: la saga no estaba en un estado que lo admita",
                eventId, origen, comprobanteId);
        } else {
            log.infof("Evento %s (%s) del comprobante %s: %s", eventId, origen, comprobanteId, this);
        }
    }
}
