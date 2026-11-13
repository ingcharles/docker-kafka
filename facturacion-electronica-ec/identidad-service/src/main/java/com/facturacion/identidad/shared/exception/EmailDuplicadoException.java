package com.facturacion.identidad.shared.exception;

import com.facturacion.commons.exception.ApiException;
import com.facturacion.identidad.shared.error.IdentidadError;

/** Ya existe un usuario con ese email. 409, {@link IdentidadError#EMAIL_DUPLICADO}. */
public class EmailDuplicadoException extends ApiException {

    public EmailDuplicadoException(String email) {
        super(IdentidadError.EMAIL_DUPLICADO, email);
    }
}
