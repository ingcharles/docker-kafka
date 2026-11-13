package com.facturacion.firma.shared.exception;

import com.facturacion.commons.exception.ApiException;
import com.facturacion.firma.shared.error.FirmaError;

/** Ya existe un establecimiento/punto de emisión activo con ese código bajo el mismo padre. 409, {@link FirmaError#CODIGO_DUPLICADO}. */
public class CodigoDuplicadoException extends ApiException {

    public CodigoDuplicadoException(String message) {
        super(FirmaError.CODIGO_DUPLICADO, message, null);
    }
}
