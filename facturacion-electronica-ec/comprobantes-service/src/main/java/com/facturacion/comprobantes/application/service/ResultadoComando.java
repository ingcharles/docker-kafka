package com.facturacion.comprobantes.application.service;

/** Qué pasó con un comando del orquestador, para que quien lo recibe lo registre y no tenga que adivinarlo. */
public enum ResultadoComando {

    /** El comprobante cambió de estado (y se guardaron los datos del paso). */
    APLICADO,

    /**
     * El comprobante ya estaba en ese estado o más adelante (comando repetido o atrasado): no se tocó nada. Un comando
     * que se SALTA pasos no llega aquí — falla, porque descartarlo dejaría el comprobante atascado.
     */
    IGNORADO_TRANSICION_NO_PERMITIDA,
    /** REENVIAR_NOTIFICACION: se volvió a publicar ComprobanteAutorizado para que notificacion-service reenvíe el correo. */
    REENVIO_PUBLICADO,
    /** REENVIAR_NOTIFICACION: el comprobante ya no se puede reenviar (sin RIDE, sin correo o no autorizado); no se publicó nada. */
    REENVIO_NO_APLICA
}
