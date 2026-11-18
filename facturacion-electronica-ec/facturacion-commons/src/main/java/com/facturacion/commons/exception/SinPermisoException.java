package com.facturacion.commons.exception;

import com.facturacion.commons.error.ErrorCatalogo;

/** La api-key es válida pero su rol o su empresa no permiten la operación (403). El texto sale de la entrada del catálogo. */
public class SinPermisoException extends ApiException {

    public SinPermisoException(ErrorCatalogo error, Object... parametros) {
        super(error, parametros);
    }
}
