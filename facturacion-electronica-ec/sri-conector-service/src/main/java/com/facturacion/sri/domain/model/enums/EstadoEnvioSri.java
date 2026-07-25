package com.facturacion.sri.domain.model.enums;

/**
 * PENDIENTE_ENVIO: creado, todavía no se llamó a Recepción — nunca se persiste en este estado
 * (ver SriConectorService.procesarComando, la llamada a Recepción es parte del mismo comando).
 * RECHAZADO_RECEPCION: el WS de Recepción devolvió {@code estado=DEVUELTA} (error de esquema/firma) — terminal, no se hace polling.
 * RECIBIDO: Recepción devolvió {@code estado=RECIBIDA} — el poller de autorización empieza a consultar.
 * AUTORIZADO: el WS de Autorización devolvió {@code estado=AUTORIZADO} — terminal feliz, {@code xml_autorizado} guardado.
 * NO_AUTORIZADO: el WS de Autorización devolvió {@code estado=NO AUTORIZADO} — terminal, corregible reintentando con nueva clave de acceso.
 * ERROR_CONSULTA: se agotaron los intentos de poll sin una respuesta AUTORIZADO/NO AUTORIZADO — requiere intervención manual.
 */
public enum EstadoEnvioSri {
    PENDIENTE_ENVIO,
    RECHAZADO_RECEPCION,
    RECIBIDO,
    AUTORIZADO,
    NO_AUTORIZADO,
    ERROR_CONSULTA
}
