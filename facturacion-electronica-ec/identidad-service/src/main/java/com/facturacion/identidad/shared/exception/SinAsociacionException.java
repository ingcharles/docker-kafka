package com.facturacion.identidad.shared.exception;

public class SinAsociacionException extends RuntimeException {

    public SinAsociacionException(String ruc) {
        super("El usuario no está asociado al ruc " + ruc + " — asóciate primero vía POST /usuarios/me/empresas");
    }
}
