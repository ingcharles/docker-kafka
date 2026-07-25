package com.facturacion.inventario.shared.exception;

import java.util.UUID;

/** Se intentó una operación de existencias (kardex, reserva, imagen de stock) sobre un ítem tipo SERVICIO. */
public class ItemNoEsProductoException extends RuntimeException {

    public ItemNoEsProductoException(UUID itemId) {
        super("El ítem " + itemId + " es un SERVICIO — no maneja existencias ni kardex");
    }
}
