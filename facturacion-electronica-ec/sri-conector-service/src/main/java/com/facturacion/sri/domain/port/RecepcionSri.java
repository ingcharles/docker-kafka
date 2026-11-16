package com.facturacion.sri.domain.port;

import io.smallrye.mutiny.Uni;

import com.facturacion.sri.domain.model.enums.AmbienteSri;

/** Puerto de salida: servicio de Recepción de comprobantes del SRI. */
public interface RecepcionSri {

    Uni<RecepcionResultado> validarComprobante(String xmlFirmado, AmbienteSri ambiente, String urlOverride);
}
