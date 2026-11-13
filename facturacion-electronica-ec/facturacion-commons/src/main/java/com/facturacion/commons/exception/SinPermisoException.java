package com.facturacion.commons.exception;

import com.facturacion.commons.error.ErrorCatalogo;
import com.facturacion.commons.error.ErrorComun;

/** La api-key es válida pero su rol o su empresa no permiten la operación (403): {@link ErrorComun#SIN_PERMISO}. */
public class SinPermisoException extends ApiException {

    public SinPermisoException(String mensaje) {
        super(ErrorComun.SIN_PERMISO, mensaje, null);
    }

    public SinPermisoException(ErrorCatalogo error, Object... parametros) {
        super(error, parametros);
    }
}
