package com.facturacion.comprobantes.domain.model.enums;

/**
 * BORRADOR: creado, {@code xml_generado} guardado, sin firmar todavía.
 * FIRMADO: {@code xml_firmado} guardado (respuesta de firma-electronica-service).
 * FIRMA_FALLIDA: el emisor no tiene certificado activo, o algo falló al firmar — estado terminal de error, corregible reintentando.
 * AUTORIZADO: {@code xml_autorizado} + número/fecha de autorización + el RIDE en PDF guardados — el SRI aceptó el comprobante.
 * <p>
 * IMPORTANTE para sri-conector-service (quien manda el comando {@code MARCAR_AUTORIZADO}):
 * {@code xml_autorizado} <b>no</b> debe ser una copia de {@code xml_firmado}. Debe ser el
 * bloque {@code <autorizacion>} completo tal como lo devuelve el WSDL real de Autorización
 * del SRI (verificado contra {@code AutorizacionComprobantesOffline?wsdl} en celcer.sri.gob.ec):
 * {@code <estado>}, {@code <numeroAutorizacion>}, {@code <fechaAutorizacion>}, {@code <ambiente>}
 * y {@code <comprobante>} (CDATA con el mismo XML firmado — el SRI no lo modifica, solo lo
 * envuelve). Que el contenido del {@code <comprobante>} interno coincida byte a byte con
 * {@code xml_firmado} es correcto y esperado; lo que sería un bug es guardar solo eso sin el
 * envoltorio {@code <autorizacion>} con sus metadatos.
 * NO_AUTORIZADO: {@code motivo_rechazo} guardado — se corrige y se reenvía con la misma clave de acceso.
 * NOTIFICADO: ya se envió por correo al receptor — estado terminal feliz.
 * ANULADO: fuera de alcance de esta versión (requiere el flujo de anulación formal del SRI, ver ARQUITECTURA.md) — el valor existe para no tener que migrar el enum más adelante.
 */
public enum EstadoComprobante {
    BORRADOR,
    FIRMADO,
    FIRMA_FALLIDA,
    AUTORIZADO,
    NO_AUTORIZADO,
    NOTIFICADO,
    ANULADO
}
