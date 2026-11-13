package com.facturacion.inventario.shared.exception;

import com.facturacion.commons.exception.ApiException;
import com.facturacion.inventario.shared.error.CodigoError;

/** La imagen subida está vacía, es muy grande o no es de un tipo admitido. 400, {@link CodigoError#IMAGEN_INVALIDA}. */
public class ImagenInvalidaException extends ApiException {

    public ImagenInvalidaException(String motivo) {
        super(CodigoError.IMAGEN_INVALIDA, motivo);
    }
}
