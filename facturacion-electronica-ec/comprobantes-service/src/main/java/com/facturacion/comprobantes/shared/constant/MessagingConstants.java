package com.facturacion.comprobantes.shared.constant;

public final class MessagingConstants {

    public static final String COMPROBANTE_EVENTS_TOPIC = "facturacion.comprobante.eventos.v1";

    public static final String EVENT_TYPE_COMPROBANTE_CREADO = "ComprobanteCreado";
    public static final String EVENT_TYPE_COMPROBANTE_AUTORIZADO = "ComprobanteAutorizado";

    // Comandos que puede mandar el orquestador vía facturacion.comprobante.comandos.v1
    public static final String COMANDO_MARCAR_FIRMADO = "MARCAR_FIRMADO";
    public static final String COMANDO_MARCAR_FIRMA_FALLIDA = "MARCAR_FIRMA_FALLIDA";
    public static final String COMANDO_MARCAR_AUTORIZADO = "MARCAR_AUTORIZADO";
    public static final String COMANDO_MARCAR_NO_AUTORIZADO = "MARCAR_NO_AUTORIZADO";
    public static final String COMANDO_MARCAR_NOTIFICADO = "MARCAR_NOTIFICADO";
    public static final String COMANDO_MARCAR_STOCK_INSUFICIENTE = "MARCAR_STOCK_INSUFICIENTE";

    private MessagingConstants() {
    }
}
