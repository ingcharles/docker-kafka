package com.facturacion.sri.shared.constant;

public final class MessagingConstants {

    public static final String SRI_EVENTS_TOPIC = "facturacion.sri.eventos.v1";

    public static final String EVENT_TYPE_SRI_RECIBIDO = "SriRecibido";
    public static final String EVENT_TYPE_SRI_RECHAZADO_RECEPCION = "SriRechazadoRecepcion";
    public static final String EVENT_TYPE_SRI_AUTORIZADO = "SriAutorizado";
    public static final String EVENT_TYPE_SRI_NO_AUTORIZADO = "SriNoAutorizado";

    private MessagingConstants() {
    }
}
