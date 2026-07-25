package com.facturacion.orquestacion.shared.constant;

/**
 * Todos los nombres de tópicos y discriminadores de comando/evento que este
 * servicio toca — es el único que conoce los cuatro dominios a la vez
 * (comprobante, firma, sri, notificacion). No hay comando hacia
 * notificacion-service: ese servicio consume ComprobanteAutorizado
 * directo de comprobante.eventos.v1, sin pasar por acá.
 */
public final class MessagingConstants {

    public static final String COMPROBANTE_EVENTS_TOPIC = "facturacion.comprobante.eventos.v1";
    public static final String COMPROBANTE_COMMANDS_TOPIC = "facturacion.comprobante.comandos.v1";
    public static final String FIRMA_COMMANDS_TOPIC = "facturacion.firma.comandos.v1";
    public static final String FIRMA_EVENTS_TOPIC = "facturacion.firma.eventos.v1";
    public static final String SRI_COMMANDS_TOPIC = "facturacion.sri.comandos.v1";
    public static final String SRI_EVENTS_TOPIC = "facturacion.sri.eventos.v1";
    public static final String NOTIFICACION_EVENTS_TOPIC = "facturacion.notificacion.eventos.v1";

    public static final String EVENT_TYPE_COMPROBANTE_CREADO = "ComprobanteCreado";
    public static final String EVENT_TYPE_COMPROBANTE_AUTORIZADO = "ComprobanteAutorizado";

    public static final String EVENT_TYPE_COMPROBANTE_FIRMADO = "ComprobanteFirmado";
    public static final String EVENT_TYPE_FIRMA_FALLIDA = "FirmaFallida";

    public static final String EVENT_TYPE_SRI_RECIBIDO = "SriRecibido";
    public static final String EVENT_TYPE_SRI_RECHAZADO_RECEPCION = "SriRechazadoRecepcion";
    public static final String EVENT_TYPE_SRI_AUTORIZADO = "SriAutorizado";
    public static final String EVENT_TYPE_SRI_NO_AUTORIZADO = "SriNoAutorizado";

    public static final String EVENT_TYPE_NOTIFICACION_ENVIADA = "NotificacionEnviada";
    public static final String EVENT_TYPE_NOTIFICACION_FALLIDA = "NotificacionFallida";

    public static final String COMANDO_MARCAR_FIRMADO = "MARCAR_FIRMADO";
    public static final String COMANDO_MARCAR_FIRMA_FALLIDA = "MARCAR_FIRMA_FALLIDA";
    public static final String COMANDO_MARCAR_AUTORIZADO = "MARCAR_AUTORIZADO";
    public static final String COMANDO_MARCAR_NO_AUTORIZADO = "MARCAR_NO_AUTORIZADO";
    public static final String COMANDO_MARCAR_NOTIFICADO = "MARCAR_NOTIFICADO";

    private MessagingConstants() {
    }
}
