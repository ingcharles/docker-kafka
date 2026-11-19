package com.facturacion.comprobantes.domain.port;

/** Resume el contenido de una solicitud en una huella estable: el mismo contenido da siempre la misma huella. */
public interface CalculadorHuella {

    /** Huella (SHA-256 en hexadecimal) del texto que identifica la solicitud. */
    String calcularHuella(Object solicitud);
}
