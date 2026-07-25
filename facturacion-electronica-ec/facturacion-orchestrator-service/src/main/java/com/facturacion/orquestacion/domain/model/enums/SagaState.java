package com.facturacion.orquestacion.domain.model.enums;

/**
 * Ciclo de vida de la saga de un comprobante. A diferencia de la saga de
 * ecommerce (que compensa reservas/pagos si algo falla), acá no hay nada
 * que compensar: firmar, enviar al SRI y notificar son pasos que o bien no
 * tienen efecto colateral que deshacer (firmar, notificar) o ya son
 * definitivos por naturaleza fiscal (una vez que el SRI autoriza, el
 * comprobante es legalmente válido pase lo que pase después) — así que
 * FIRMA_FALLIDA/RECHAZADO_RECEPCION/NO_AUTORIZADO son estados terminales de
 * error sin ningún comando de compensación, y NOTIFICACION_FALLIDA es un
 * terminal de error que ni siquiera toca el estado de comprobantes-service
 * (el comprobante ya está AUTORIZADO, sigue siéndolo aunque el correo falle).
 */
public enum SagaState {
    CREADO,
    FIRMADO,
    FIRMA_FALLIDA,
    RECHAZADO_RECEPCION,
    AUTORIZADO,
    NO_AUTORIZADO,
    NOTIFICADO,
    NOTIFICACION_FALLIDA
}
