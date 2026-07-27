package com.facturacion.identidad.shared.exception;

/** Conflicto genérico (409) que no encaja en una excepción más específica — ver ApiKeyService. */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String message) {
        super(message);
    }
}
