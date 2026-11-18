package com.facturacion.commons.exception;

import com.facturacion.commons.error.ErrorCatalogo;

/** El estado actual del recurso no admite la operación, o ya existe (409). El texto sale de la entrada del catálogo. */
public class ConflictoException extends ApiException {

    public ConflictoException(ErrorCatalogo error, Object... parametros) {
        super(error, parametros);
    }
}
