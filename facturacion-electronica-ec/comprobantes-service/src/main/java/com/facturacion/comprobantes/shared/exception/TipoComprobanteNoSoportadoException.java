package com.facturacion.comprobantes.shared.exception;

import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;

/** Los otros 5 tipos (fuera de Factura) todavía no tienen un GeneradorXmlComprobante registrado — ver ARQUITECTURA.md. */
public class TipoComprobanteNoSoportadoException extends RuntimeException {

    public TipoComprobanteNoSoportadoException(TipoComprobante tipo) {
        super("El tipo de comprobante " + tipo + " todavía no está soportado en esta versión");
    }
}
