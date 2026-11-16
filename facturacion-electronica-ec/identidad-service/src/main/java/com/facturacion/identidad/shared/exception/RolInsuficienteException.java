package com.facturacion.identidad.shared.exception;

import com.facturacion.commons.exception.ApiException;
import com.facturacion.identidad.shared.error.IdentidadError;

/**
 * El usuario autenticado no tiene el rol ADMIN sobre el ruc cuyos miembros intenta gestionar. 403,
 * {@link IdentidadError#ROL_INSUFICIENTE}.
 */
public class RolInsuficienteException extends ApiException {

    public RolInsuficienteException(String ruc) {
        super(IdentidadError.ROL_INSUFICIENTE, ruc);
    }
}
