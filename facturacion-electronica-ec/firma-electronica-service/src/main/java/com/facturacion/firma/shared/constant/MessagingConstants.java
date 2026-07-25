package com.facturacion.firma.shared.constant;

/** Nombres de tópicos y discriminadores de tipo de evento/comando que este servicio produce o consume. */
public final class MessagingConstants {

    public static final String EMISOR_EVENTS_TOPIC = "facturacion.emisor.eventos.v1";
    public static final String FIRMA_EVENTS_TOPIC = "facturacion.firma.eventos.v1";

    public static final String EVENT_TYPE_EMISOR_REGISTRADO = "EmisorRegistrado";
    public static final String EVENT_TYPE_EMISOR_ACTUALIZADO = "EmisorActualizado";

    public static final String EVENT_TYPE_COMPROBANTE_FIRMADO = "ComprobanteFirmado";
    public static final String EVENT_TYPE_FIRMA_FALLIDA = "FirmaFallida";

    private MessagingConstants() {
    }
}
