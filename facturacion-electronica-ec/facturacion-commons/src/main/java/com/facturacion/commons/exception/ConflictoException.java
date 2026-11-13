package com.facturacion.commons.exception;

import com.facturacion.commons.error.ErrorCatalogo;
import com.facturacion.commons.error.ErrorComun;

/** El estado actual del recurso no admite la operación, o ya existe (409): {@link ErrorComun#CONFLICTO}. */
public class ConflictoException extends ApiException {

    public ConflictoException(String mensaje) {
        super(ErrorComun.CONFLICTO, mensaje, null);
    }

    public ConflictoException(ErrorCatalogo error, Object... parametros) {
        super(error, parametros);
    }
}
