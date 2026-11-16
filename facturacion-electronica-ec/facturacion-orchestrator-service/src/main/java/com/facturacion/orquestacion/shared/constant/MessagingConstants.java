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
    public static final String COMANDO_MARCAR_STOCK_INSUFICIENTE = "MARCAR_STOCK_INSUFICIENTE";

    // Inventario: solo participa si el descuento de inventario está habilitado (orquestacion.inventario.descuento.habilitado).
    public static final String INVENTARIO_COMMANDS_TOPIC = "facturacion.inventario.comandos.v1";
    public static final String INVENTARIO_EVENTS_TOPIC = "facturacion.inventario.eventos.v1";
    public static final String COMANDO_STOCK_RESERVAR = "RESERVAR";
    public static final String COMANDO_STOCK_CONFIRMAR = "CONFIRMAR";
    public static final String COMANDO_STOCK_LIBERAR = "LIBERAR";
    public static final String EVENT_TYPE_STOCK_RESERVADO = "StockReservado";
    public static final String EVENT_TYPE_STOCK_NO_DISPONIBLE = "StockNoDisponible";
    public static final String TIPO_COMPROBANTE_FACTURA = "FACTURA";

    private MessagingConstants() {
    }
}
