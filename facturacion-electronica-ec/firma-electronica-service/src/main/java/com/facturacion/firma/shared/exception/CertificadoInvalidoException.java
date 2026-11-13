package com.facturacion.firma.shared.exception;

import com.facturacion.commons.exception.ApiException;
import com.facturacion.firma.shared.error.FirmaError;

/**
 * El archivo subido no es un .p12/PKCS12 válido, o la contraseña dada no lo abre. 400,
 * {@link FirmaError#CERTIFICADO_INVALIDO}.
 */
public class CertificadoInvalidoException extends ApiException {

    public CertificadoInvalidoException(String message, Throwable cause) {
        super(FirmaError.CERTIFICADO_INVALIDO, message, cause);
    }

    public CertificadoInvalidoException(String message) {
        super(FirmaError.CERTIFICADO_INVALIDO, message, null);
    }
}
