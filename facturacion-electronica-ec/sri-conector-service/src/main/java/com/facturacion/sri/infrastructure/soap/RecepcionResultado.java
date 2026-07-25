package com.facturacion.sri.infrastructure.soap;

/**
 * {@code estado}: RECIBIDA (el sobre pasó validación de esquema/firma, hay
 * que consultar Autorización más tarde) o DEVUELTA (rechazado de una,
 * {@code mensajes} trae el motivo — nunca llega a Autorización).
 */
public record RecepcionResultado(String estado, String mensajes) {

    public boolean recibida() {
        return "RECIBIDA".equalsIgnoreCase(estado);
    }
}
