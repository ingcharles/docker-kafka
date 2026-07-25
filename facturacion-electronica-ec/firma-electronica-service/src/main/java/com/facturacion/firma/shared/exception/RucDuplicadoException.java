package com.facturacion.firma.shared.exception;

/** Ya existe un emisor activo registrado con ese RUC. */
public class RucDuplicadoException extends RuntimeException {

    public RucDuplicadoException(String ruc) {
        super("Ya existe un emisor registrado con el RUC " + ruc);
    }
}
