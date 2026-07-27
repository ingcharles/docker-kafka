package com.facturacion.inventario.shared.exception;

public class SinPermisoException extends RuntimeException {

    public SinPermisoException(String message) {
        super(message);
    }
}
