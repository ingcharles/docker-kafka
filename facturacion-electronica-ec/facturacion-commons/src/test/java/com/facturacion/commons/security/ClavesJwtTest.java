package com.facturacion.commons.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import org.junit.jupiter.api.Test;

class ClavesJwtTest {

    private static KeyPair par(String algoritmo) throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance(algoritmo);
        if ("RSA".equals(algoritmo)) {
            generador.initialize(2048);
        }
        return generador.generateKeyPair();
    }

    private static String pem(String etiqueta, byte[] der, String salto) {
        return "-----BEGIN " + etiqueta + "-----" + salto + Base64.getMimeEncoder(64, salto.getBytes()).encodeToString(der)
            + salto + "-----END " + etiqueta + "-----" + salto;
    }

    @Test
    void leeEd25519EnPemConSaltosDeLineaReales() throws Exception {
        KeyPair par = par("Ed25519");

        assertEquals(par.getPublic(), ClavesJwt.leerPublica(pem("PUBLIC KEY", par.getPublic().getEncoded(), "\n")));
        assertEquals(par.getPrivate(), ClavesJwt.leerPrivada(pem("PRIVATE KEY", par.getPrivate().getEncoded(), "\n")));
    }

    @Test
    void leeElPemEnUnaSolaLineaConSaltosLiteralesComoLoDejaUnaVariableDeEntorno() throws Exception {
        KeyPair par = par("Ed25519");
        String enUnaLinea = pem("PUBLIC KEY", par.getPublic().getEncoded(), "\n").replace("\n", "\\n");

        assertEquals(par.getPublic(), ClavesJwt.leerPublica(enUnaLinea));
    }

    @Test
    void leeSoloElBase64SinEncabezados() throws Exception {
        KeyPair par = par("Ed25519");

        assertEquals(par.getPublic(), ClavesJwt.leerPublica(Base64.getEncoder().encodeToString(par.getPublic().getEncoded())));
    }

    @Test
    void tambienAceptaRsa() throws Exception {
        KeyPair par = par("RSA");

        assertEquals(par.getPublic(), ClavesJwt.leerPublica(pem("PUBLIC KEY", par.getPublic().getEncoded(), "\n")));
        assertEquals(par.getPrivate(), ClavesJwt.leerPrivada(pem("PRIVATE KEY", par.getPrivate().getEncoded(), "\n")));
    }

    @Test
    void unaClaveVaciaOInvalidaFallaAlArrancarConUnMensajeClaro() {
        assertThrows(IllegalArgumentException.class, () -> ClavesJwt.leerPublica(null));
        assertThrows(IllegalArgumentException.class, () -> ClavesJwt.leerPublica("   "));
        assertThrows(IllegalArgumentException.class, () -> ClavesJwt.leerPublica("esto no es base64 ***"));
        assertThrows(IllegalArgumentException.class,
            () -> ClavesJwt.leerPublica(Base64.getEncoder().encodeToString("no es una llave".getBytes())));
        assertThrows(IllegalArgumentException.class, () -> ClavesJwt.leerPrivada("QUJD"));
    }
}
