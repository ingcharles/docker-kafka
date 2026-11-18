package com.facturacion.commons.exception;

import com.facturacion.commons.error.ErrorCatalogo;
import com.facturacion.commons.error.ErrorComun;
import com.facturacion.commons.error.Mensajes;

/** Falta el token, es inválido, venció o no es del tipo esperado (401): entradas {@link ErrorComun#TOKEN_INVALIDO} y afines. */
public class TokenInvalidoException extends ApiException {

    public TokenInvalidoException(ErrorCatalogo error, Object... parametros) {
        super(error, parametros);
    }

    /** Conserva la causa (p. ej. la excepción de JJWT) para el log sin exponerla en el mensaje al cliente. */
    public TokenInvalidoException(Throwable causa, ErrorCatalogo error, Object... parametros) {
        super(error, Mensajes.obtenerTexto(error, parametros), causa);
    }
}
