package com.facturacion.commons.exception;

import com.facturacion.commons.error.ErrorCatalogo;
import com.facturacion.commons.error.ErrorComun;

/** Falta el token, es inválido, venció o no es del tipo esperado (401): {@link ErrorComun#TOKEN_INVALIDO}. */
public class TokenInvalidoException extends ApiException {

    public TokenInvalidoException(String mensaje) {
        super(ErrorComun.TOKEN_INVALIDO, mensaje, null);
    }

    /** Conserva la causa (p. ej. la excepción de JJWT) para el log sin exponerla en el mensaje al cliente. */
    public TokenInvalidoException(String mensaje, Throwable causa) {
        super(ErrorComun.TOKEN_INVALIDO, mensaje, causa);
    }

    public TokenInvalidoException(ErrorCatalogo error, Object... parametros) {
        super(error, parametros);
    }
}
