package com.facturacion.firma.shared.exception;

/** Ya existe un establecimiento/punto de emisión activo con ese código bajo el mismo padre. */
public class CodigoDuplicadoException extends RuntimeException {

    public CodigoDuplicadoException(String message) {
        super(message);
    }
}
