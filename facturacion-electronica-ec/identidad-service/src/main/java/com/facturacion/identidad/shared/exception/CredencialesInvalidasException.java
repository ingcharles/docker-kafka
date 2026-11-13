package com.facturacion.identidad.shared.exception;

import com.facturacion.commons.exception.ApiException;
import com.facturacion.identidad.shared.error.IdentidadError;

/** Email o contraseña incorrectos (login, cambio de contraseña). 401, {@link IdentidadError#CREDENCIALES_INVALIDAS}. */
public class CredencialesInvalidasException extends ApiException {

    public CredencialesInvalidasException(String motivo) {
        super(IdentidadError.CREDENCIALES_INVALIDAS, motivo);
    }
}
