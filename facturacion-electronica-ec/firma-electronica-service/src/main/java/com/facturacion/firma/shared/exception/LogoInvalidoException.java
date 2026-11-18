package com.facturacion.firma.shared.exception;

import com.facturacion.commons.exception.ApiException;
import com.facturacion.firma.shared.error.FirmaError;

/** El logo subido está vacío, pesa más de lo permitido o no es PNG/JPEG. 400; la entrada del catálogo dice cuál. */
public class LogoInvalidoException extends ApiException {

    public LogoInvalidoException(FirmaError error, Object... parametros) {
        super(error, parametros);
    }
}
