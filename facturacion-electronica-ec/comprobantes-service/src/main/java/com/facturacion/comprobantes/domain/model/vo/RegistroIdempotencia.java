package com.facturacion.comprobantes.domain.model.vo;

import java.util.UUID;

/**
 * Lo que quedó guardado de la primera solicitud con una clave de idempotencia: la huella de su contenido y el
 * comprobante que creó.
 */
public record RegistroIdempotencia(String huella, UUID comprobanteId) {
}
