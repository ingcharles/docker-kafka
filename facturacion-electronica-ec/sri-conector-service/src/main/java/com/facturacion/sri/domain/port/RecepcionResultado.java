package com.facturacion.sri.domain.port;

/**
 * {@code estado}: RECIBIDA (el sobre pasó validación de esquema/firma, hay
 * que consultar Autorización más tarde) o DEVUELTA (rechazado de una,
 * {@code mensajes} trae el motivo — nunca llega a Autorización).
 * <p>
 * {@code claveYaRegistrada}: el SRI devolvió el comprobante porque YA lo tiene (identificador 43, clave de acceso
 * registrada, o 70, en procesamiento). Pasa cuando un envío llegó al SRI pero no se alcanzó a guardar su resultado
 * (p. ej. falló la base) y el reintento lo manda otra vez: no es un rechazo, el comprobante sí está en el SRI, así
 * que se sigue con la consulta de autorización.
 */
public record RecepcionResultado(String estado, String mensajes, boolean claveYaRegistrada) {

    public RecepcionResultado(String estado, String mensajes) {
        this(estado, mensajes, false);
    }

    public boolean recibida() {
        return "RECIBIDA".equalsIgnoreCase(estado) || claveYaRegistrada;
    }
}
