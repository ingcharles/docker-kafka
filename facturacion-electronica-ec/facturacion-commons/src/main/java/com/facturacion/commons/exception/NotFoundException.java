package com.facturacion.commons.exception;

import com.facturacion.commons.error.ErrorCatalogo;

/** El recurso pedido no existe (o es de otra empresa y no se revela). 404; el texto sale de la entrada del catálogo. */
public class NotFoundException extends ApiException {

    public NotFoundException(ErrorCatalogo error, Object... parametros) {
        super(error, parametros);
    }
}
