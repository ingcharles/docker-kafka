package com.facturacion.comprobantes.application.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Mensaje publicado en {@code facturacion.comprobante.eventos.v1} justo
 * después de {@code MARCAR_AUTORIZADO}, solo cuando el comprobante tiene un
 * {@code compradorCorreo} — es lo que notificacion-service consume para
 * mandar el correo real. Lleva el RIDE ya generado en base64 y el
 * {@code xmlAutorizado} (el envoltorio `<autorizacion>` completo devuelto
 * por el SRI) tal cual, sin base64 — es texto XML, no binario — (no una
 * referencia a ellos): notificacion-service no puede llamar de vuelta a este
 * servicio (cero REST entre servicios) ni regenerar el RIDE ni volver a
 * pedir el XML por su cuenta, así que ambos viajan embebidos en el evento —
 * mismo criterio que {@code ComprobanteCreadoEventMessage} embebe
 * {@code xmlGenerado}.
 */
public record ComprobanteAutorizadoEventMessage(
    UUID eventId,
    UUID comprobanteId,
    String eventType,
    String empresaRuc,
    String claveAcceso,
    String tipoComprobante,
    String compradorCorreo,
    String compradorRazonSocial,
    String ridePdfBase64,
    String xmlAutorizado,
    Instant occurredAt) {
}
