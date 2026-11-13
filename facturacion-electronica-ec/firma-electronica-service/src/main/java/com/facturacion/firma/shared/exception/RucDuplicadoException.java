package com.facturacion.firma.shared.exception;

import com.facturacion.commons.exception.ApiException;
import com.facturacion.firma.shared.error.FirmaError;

/** Ya existe una empresa activa registrada con ese RUC. 409, {@link FirmaError#RUC_DUPLICADO}. */
public class RucDuplicadoException extends ApiException {

    public RucDuplicadoException(String ruc) {
        super(FirmaError.RUC_DUPLICADO, ruc);
    }
}
