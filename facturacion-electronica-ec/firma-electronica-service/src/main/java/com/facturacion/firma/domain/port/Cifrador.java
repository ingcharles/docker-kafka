package com.facturacion.firma.domain.port;

/** Puerto de salida: cifrado en reposo del certificado .p12 y su contraseña. */
public interface Cifrador {

    byte[] cifrar(byte[] datosClaros);

    byte[] descifrar(byte[] datosCifrados);
}
