package com.facturacion.inventario.shared.exception;

import java.util.UUID;

import com.facturacion.commons.exception.ApiException;
import com.facturacion.inventario.shared.error.CodigoError;

/** No hay existencias suficientes para la salida o la reserva pedida. 409, {@link CodigoError#STOCK_INSUFICIENTE}. */
public class StockInsuficienteException extends ApiException {

    public StockInsuficienteException(UUID itemId) {
        super(CodigoError.STOCK_INSUFICIENTE, itemId);
    }
}
