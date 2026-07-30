package com.facturacion.identidad.shared.exception;

/** El usuario autenticado no tiene el rol ADMIN sobre el ruc cuyos miembros intenta gestionar. */
public class RolInsuficienteException extends RuntimeException {

    public RolInsuficienteException(String ruc) {
        super("Se requiere el rol ADMIN sobre el ruc " + ruc + " para gestionar sus miembros");
    }
}
