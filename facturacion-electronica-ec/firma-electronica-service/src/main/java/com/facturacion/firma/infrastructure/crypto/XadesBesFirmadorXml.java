package com.facturacion.firma.infrastructure.crypto;

import jakarta.enterprise.context.ApplicationScoped;

import com.facturacion.firma.domain.port.FirmadorXml;

/** Adaptador del puerto {@link FirmadorXml} sobre {@link XadesBesFirmador}. */
@ApplicationScoped
public class XadesBesFirmadorXml implements FirmadorXml {

    @Override
    public byte[] firmar(byte[] xmlSinFirmar, byte[] contenidoP12, char[] contrasena) {
        return XadesBesFirmador.firmar(xmlSinFirmar, contenidoP12, contrasena);
    }
}
