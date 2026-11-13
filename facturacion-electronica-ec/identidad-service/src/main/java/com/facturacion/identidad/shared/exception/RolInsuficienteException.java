package com.facturacion.identidad.shared.exception;

import com.facturacion.commons.error.ErrorCatalogo;
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

    private RolInsuficienteException(ErrorCatalogo error, String ruc) {
        super(error, ruc);
    }

    /** Un usuario ajeno intenta asociarse a una empresa que ya tiene administradores: debe agregarlo uno de ellos. */
    public static RolInsuficienteException empresaYaTieneAdministradores(String ruc) {
        return new RolInsuficienteException(IdentidadError.EMPRESA_CON_ADMINISTRADORES, ruc);
    }
}
