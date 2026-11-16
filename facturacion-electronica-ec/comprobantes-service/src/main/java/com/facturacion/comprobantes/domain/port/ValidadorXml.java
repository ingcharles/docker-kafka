package com.facturacion.comprobantes.domain.port;

import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;

/** Puerto de salida: valida el XML generado contra el XSD oficial del SRI; lanza excepción si no cumple. */
public interface ValidadorXml {

    void validar(TipoComprobante tipo, String xml);
}
