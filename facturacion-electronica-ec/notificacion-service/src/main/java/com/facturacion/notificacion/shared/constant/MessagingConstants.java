package com.facturacion.notificacion.shared.constant;

public final class MessagingConstants {

    public static final String NOTIFICACION_EVENTS_TOPIC = "facturacion.notificacion.eventos.v1";

    // El único eventType de facturacion.comprobante.eventos.v1 que a este
    // servicio le importa — el resto (hoy: ComprobanteCreado) se ignora,
    // ver ComprobanteEventConsumer.
    public static final String EVENT_TYPE_COMPROBANTE_AUTORIZADO = "ComprobanteAutorizado";

    public static final String EVENT_TYPE_NOTIFICACION_ENVIADA = "NotificacionEnviada";
    public static final String EVENT_TYPE_NOTIFICACION_FALLIDA = "NotificacionFallida";

    private MessagingConstants() {
    }
}
