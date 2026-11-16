package com.facturacion.comprobantes.domain.port;

import com.facturacion.comprobantes.domain.model.entity.Comprobante;

/** Puerto de salida: genera el XML (sin firmar) del comprobante según su tipo. */
public interface GeneradorXml {

    String generar(Comprobante comprobante);
}
