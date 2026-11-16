package com.facturacion.sri.domain.port;

import java.time.Instant;

/**
 * {@code estado}: null/vacío mientras {@code numeroComprobantes=0} (el SRI
 * todavía no procesó el sobre — hay que seguir consultando), "AUTORIZADO",
 * "NO AUTORIZADO", o cualquier otro texto que el SRI use como estado
 * transitorio (ver Tabla de estados del Anexo de Autorización).
 * {@code xmlAutorizacionCompleto} es el bloque {@code <autorizacion>}
 * entero tal como lo devolvió el SRI (estado+numeroAutorizacion+
 * fechaAutorizacion+ambiente+comprobante), listo para guardarse tal cual
 * en {@code xml_autorizado} — no es el XML del comprobante solo.
 */
public record AutorizacionResultado(
    String estado,
    String numeroAutorizacion,
    Instant fechaAutorizacion,
    String xmlAutorizacionCompleto,
    String motivoRechazo) {

    public boolean pendiente() {
        return estado == null || estado.isBlank();
    }

    public boolean autorizado() {
        return "AUTORIZADO".equalsIgnoreCase(estado);
    }
}
