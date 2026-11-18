package com.facturacion.firma.shared.exception;

import com.facturacion.commons.exception.ApiException;
import com.facturacion.firma.shared.error.FirmaError;

/**
 * El certificado no sirve para firmar: no es un .p12/PKCS12 válido, la contraseña no lo abre, está vencido o la empresa
 * no tiene uno activo. 400; la entrada concreta del catálogo ({@link FirmaError}) dice cuál de esos casos es.
 */
public class CertificadoInvalidoException extends ApiException {

    public CertificadoInvalidoException(FirmaError error, Object... parametros) {
        super(error, parametros);
    }

    public CertificadoInvalidoException(FirmaError error, Throwable causa, Object... parametros) {
        super(error, causa, parametros);
    }
}
