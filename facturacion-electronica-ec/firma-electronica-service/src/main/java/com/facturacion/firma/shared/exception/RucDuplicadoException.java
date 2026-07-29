package com.facturacion.firma.shared.exception;

/** Ya existe una empresa activa registrada con ese RUC. */
public class RucDuplicadoException extends RuntimeException {

    public RucDuplicadoException(String ruc) {
        super("Ya existe una empresa registrada con el RUC " + ruc);
    }
}
