package com.facturacion.sri.domain.port;

import io.smallrye.mutiny.Uni;

import com.facturacion.sri.domain.model.enums.AmbienteSri;

/** Puerto de salida: servicio de Autorización de comprobantes del SRI. */
public interface AutorizacionSri {

    Uni<AutorizacionResultado> consultar(String claveAcceso, AmbienteSri ambiente, String urlOverride);
}
