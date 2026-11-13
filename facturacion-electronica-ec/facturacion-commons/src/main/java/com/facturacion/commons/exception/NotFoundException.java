package com.facturacion.commons.exception;

import com.facturacion.commons.error.ErrorCatalogo;
import com.facturacion.commons.error.ErrorComun;

/** El recurso pedido no existe (o es de otra empresa y no se revela). 404 por defecto: {@link ErrorComun#RECURSO_NO_ENCONTRADO}. */
public class NotFoundException extends ApiException {

    public NotFoundException(String mensaje) {
        super(ErrorComun.RECURSO_NO_ENCONTRADO, mensaje, null);
    }

    /** Con una entrada propia del catálogo del servicio (código específico); el texto sale del .properties. */
    public NotFoundException(ErrorCatalogo error, Object... parametros) {
        super(error, parametros);
    }
}
