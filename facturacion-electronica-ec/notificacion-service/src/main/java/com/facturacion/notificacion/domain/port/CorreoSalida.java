package com.facturacion.notificacion.domain.port;

import io.smallrye.mutiny.Uni;

/** Puerto de salida: envío del comprobante autorizado por correo (SMTP). */
public interface CorreoSalida {

    Uni<Void> enviarComprobante(String destinatario, String claveAcceso, String tipoComprobante, byte[] ridePdf,
            String xmlAutorizado, String asunto, String remitenteNombre);
}
