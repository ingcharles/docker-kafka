package com.facturacion.commons.exception;

import com.facturacion.commons.error.ErrorCatalogo;

/**
 * Falla de infraestructura que el cliente no puede corregir (serializar un payload, generar un XML o un PDF, cifrar,
 * contactar al SRI...). Lleva su entrada del catálogo (código estable, estado 5xx) y la causa original. El
 * {@code ApiExceptionMapper} registra el detalle y la causa en el log con ese código y al cliente solo le responde el
 * texto genérico y el código, sin exponer detalles internos. Se usa en lugar de {@code IllegalStateException} para todo
 * lo que puede fallar en ejecución; {@code IllegalStateException} queda solo para invariantes que no deberían romperse.
 */
public class ErrorInternoException extends ApiException {

    public ErrorInternoException(ErrorCatalogo error, Object... parametros) {
        super(error, parametros);
    }

    /** Conserva la causa original para el log. */
    public ErrorInternoException(Throwable causa, ErrorCatalogo error, Object... parametros) {
        super(error, causa, parametros);
    }
}
