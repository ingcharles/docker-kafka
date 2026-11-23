package com.facturacion.notificacion.domain.model.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;

import org.junit.jupiter.api.Test;

class LogoEmpresaTest {

    private static String base64(int... bytes) {
        byte[] datos = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            datos[i] = (byte) bytes[i];
        }
        return Base64.getEncoder().encodeToString(datos);
    }

    @Test
    void testDeberiaReconocerUnPngPorSusPrimerosBytes() {
        LogoEmpresa logo = LogoEmpresa.desdeBase64(base64(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A)).orElseThrow();

        assertEquals("image/png", logo.tipoContenido());
        assertEquals(6, logo.datos().length);
    }

    @Test
    void testDeberiaReconocerUnJpegPorSusPrimerosBytes() {
        assertEquals("image/jpeg", LogoEmpresa.desdeBase64(base64(0xFF, 0xD8, 0xFF, 0xE0)).orElseThrow().tipoContenido());
    }

    @Test
    void testDeberiaDevolverSinLogoSiNoHayValor() {
        assertTrue(LogoEmpresa.desdeBase64(null).isEmpty());
        assertTrue(LogoEmpresa.desdeBase64("").isEmpty());
        assertTrue(LogoEmpresa.desdeBase64("   ").isEmpty());
    }

    @Test
    void testDeberiaRechazarUnTextoQueNoEsBase64() {
        assertThrows(IllegalArgumentException.class, () -> LogoEmpresa.desdeBase64("esto no es base64 !!"));
    }

    @Test
    void testDeberiaRechazarUnArchivoQueNoEsPngNiJpeg() {
        assertThrows(IllegalArgumentException.class, () -> LogoEmpresa.desdeBase64(base64(0x47, 0x49, 0x46, 0x38)));
        assertThrows(IllegalArgumentException.class, () -> LogoEmpresa.desdeBase64(base64(0x3C, 0x73, 0x76, 0x67)));
        assertThrows(IllegalArgumentException.class, () -> LogoEmpresa.desdeBase64(base64(0x89)));
    }
}
