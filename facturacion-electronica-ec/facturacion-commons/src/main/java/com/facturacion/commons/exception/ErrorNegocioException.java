package com.facturacion.commons.exception;

import java.util.List;

import com.facturacion.commons.error.ErrorCatalogo;

/**
 * Error de negocio con código: la excepción que lanzan las reglas de cada servicio. El status, el {@code codigo} y el
 * texto salen de la entrada del catálogo ({@code CodigoError} del servicio); la respuesta la arma el
 * {@code ApiExceptionMapper}. {@code detalles} lleva los problemas individuales cuando un mismo error agrupa varios.
 */
public class ErrorNegocioException extends ApiException {

    public ErrorNegocioException(ErrorCatalogo error, Object... parametros) {
        super(error, parametros);
    }

    private ErrorNegocioException(ErrorCatalogo error, List<String> detalles, Object[] parametros) {
        super(error, detalles, parametros);
    }

    public static ErrorNegocioException conDetalles(ErrorCatalogo error, List<String> detalles, Object... parametros) {
        return new ErrorNegocioException(error, detalles, parametros);
    }

    /** Compatibilidad con el nombre que tenía antes. */
    public ErrorCatalogo getCodigo() {
        return getError();
    }
}
