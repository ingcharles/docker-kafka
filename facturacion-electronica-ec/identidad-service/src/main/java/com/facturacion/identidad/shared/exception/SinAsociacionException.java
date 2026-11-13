package com.facturacion.identidad.shared.exception;

import com.facturacion.commons.exception.ApiException;
import com.facturacion.identidad.shared.error.IdentidadError;

/** El usuario no está asociado al ruc con el que intenta operar. 403, {@link IdentidadError#SIN_ASOCIACION}. */
public class SinAsociacionException extends ApiException {

    public SinAsociacionException(String ruc) {
        super(IdentidadError.SIN_ASOCIACION, ruc);
    }
}
