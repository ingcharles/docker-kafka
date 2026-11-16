package com.facturacion.firma.shared.exception;

import com.facturacion.commons.exception.ApiException;
import com.facturacion.firma.shared.error.FirmaError;

/** El logo subido no es un PNG/JPEG válido o supera el tamaño permitido. 400, {@link FirmaError#LOGO_INVALIDO}. */
public class LogoInvalidoException extends ApiException {

    public LogoInvalidoException(String message) {
        super(FirmaError.LOGO_INVALIDO, message, null);
    }
}
