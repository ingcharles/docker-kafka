package com.facturacion.inventario.shared.exception;

import java.util.UUID;

import com.facturacion.commons.exception.ApiException;
import com.facturacion.inventario.shared.error.CodigoError;

/**
 * Se intentó una operación de existencias (kardex, reserva, imagen de stock) sobre un ítem tipo SERVICIO. 400,
 * {@link CodigoError#ITEM_NO_ES_PRODUCTO}.
 */
public class ItemNoEsProductoException extends ApiException {

    public ItemNoEsProductoException(UUID itemId) {
        super(CodigoError.ITEM_NO_ES_PRODUCTO, itemId);
    }
}
