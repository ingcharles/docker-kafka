package com.facturacion.inventario.shared.exception;

import java.util.UUID;

public class StockInsuficienteException extends RuntimeException {

    public StockInsuficienteException(UUID itemId) {
        super("Stock insuficiente para el ítem " + itemId);
    }
}
