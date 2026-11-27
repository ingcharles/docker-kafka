package com.facturacion.commons.exception;

import com.facturacion.commons.error.ErrorComun;

/** El usuario o su empresa superaron el límite de una operación: 429 con {@code Retry-After}. */
public class LimiteExcedidoException extends ApiException {

    private final long segundosEspera;

    public LimiteExcedidoException(long segundosEspera) {
        super(ErrorComun.LIMITE_EXCEDIDO, Math.max(1, segundosEspera));
        this.segundosEspera = Math.max(1, segundosEspera);
    }

    public long getSegundosEspera() {
        return segundosEspera;
    }
}
