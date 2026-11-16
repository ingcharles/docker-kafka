package com.facturacion.firma.domain.port;

/** Puerto de salida: firma un XML con XAdES-BES usando un certificado .p12. */
public interface FirmadorXml {

    byte[] firmar(byte[] xmlSinFirmar, byte[] contenidoP12, char[] contrasena);
}
