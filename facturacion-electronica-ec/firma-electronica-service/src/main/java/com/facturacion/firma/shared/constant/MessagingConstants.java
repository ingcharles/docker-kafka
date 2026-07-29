package com.facturacion.firma.shared.constant;

/** Nombres de tópicos y discriminadores de tipo de evento/comando que este servicio produce o consume. */
public final class MessagingConstants {

    public static final String EMPRESA_EVENTS_TOPIC = "facturacion.empresa.eventos.v1";
    public static final String FIRMA_EVENTS_TOPIC = "facturacion.firma.eventos.v1";
    public static final String ESTABLECIMIENTO_PUNTO_EMISION_EVENTS_TOPIC =
        "facturacion.empresa.establecimientos.eventos.v1";
    public static final String CONFIGURACION_EMPRESA_EVENTS_TOPIC = "facturacion.empresa.configuracion.eventos.v1";

    public static final String EVENT_TYPE_EMPRESA_REGISTRADA = "EmpresaRegistrada";
    public static final String EVENT_TYPE_EMPRESA_ACTUALIZADA = "EmpresaActualizada";

    public static final String EVENT_TYPE_ESTABLECIMIENTO_REGISTRADO = "EstablecimientoRegistrado";
    public static final String EVENT_TYPE_ESTABLECIMIENTO_ACTUALIZADO = "EstablecimientoActualizado";
    public static final String EVENT_TYPE_PUNTO_EMISION_REGISTRADO = "PuntoEmisionRegistrado";
    public static final String EVENT_TYPE_PUNTO_EMISION_ACTUALIZADO = "PuntoEmisionActualizado";

    public static final String EVENT_TYPE_CONFIGURACION_EMPRESA_ESTABLECIDA = "ConfiguracionEmpresaEstablecida";
    public static final String EVENT_TYPE_CONFIGURACION_EMPRESA_ELIMINADA = "ConfiguracionEmpresaEliminada";

    public static final String EVENT_TYPE_COMPROBANTE_FIRMADO = "ComprobanteFirmado";
    public static final String EVENT_TYPE_FIRMA_FALLIDA = "FirmaFallida";

    private MessagingConstants() {
    }
}
