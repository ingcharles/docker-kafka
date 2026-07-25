package com.ecommerce.inventory.shared.exception;

import java.util.UUID;

/**
 * Se lanza a mitad de un intento de reserva. Antes de que esta excepción se
 * propague hacia arriba, quien la lanza ya se encargó de compensar
 * (liberar) todo lo que se había logrado reservar hasta ese punto — ver
 * {@code StockReservationService.tryReserveAll}.
 */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(UUID productId) {
        super("Insufficient stock for product " + productId);
    }
}
