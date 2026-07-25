package com.facturacion.firma.shared.exception;

/** El archivo subido no es un .p12/PKCS12 válido, o la contraseña dada no lo abre. */
public class CertificadoInvalidoException extends RuntimeException {

    public CertificadoInvalidoException(String message, Throwable cause) {
        super(message, cause);
    }

    public CertificadoInvalidoException(String message) {
        super(message);
    }
}
