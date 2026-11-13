package com.facturacion.comprobantes.shared.exception;

import com.facturacion.commons.exception.ApiException;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.shared.error.CodigoError;

/**
 * No hay un {@code GeneradorXmlComprobante} registrado para ese tipo — ver ARQUITECTURA.md. 400 con el código
 * {@link CodigoError#TIPO_COMPROBANTE_NO_SOPORTADO}; la respuesta la arma el {@code ApiExceptionMapper} del jar común.
 */
public class TipoComprobanteNoSoportadoException extends ApiException {

    public TipoComprobanteNoSoportadoException(TipoComprobante tipo) {
        super(CodigoError.TIPO_COMPROBANTE_NO_SOPORTADO, tipo);
    }
}
