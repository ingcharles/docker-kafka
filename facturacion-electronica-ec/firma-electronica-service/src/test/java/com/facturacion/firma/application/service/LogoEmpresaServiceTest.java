package com.facturacion.firma.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.facturacion.firma.shared.exception.LogoInvalidoException;

/** El tipo del logo se decide por sus primeros bytes y el tamaño tiene tope: el cliente no manda el tipo. */
class LogoEmpresaServiceTest {

    private static byte[] conCabecera(byte[] cabecera, int total) {
        byte[] bytes = new byte[total];
        System.arraycopy(cabecera, 0, bytes, 0, cabecera.length);
        return bytes;
    }

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};

    @Test
    void testDeberiaReconocerUnPngYUnJpegPorSuCabecera() {
        assertEquals("image/png", LogoEmpresaService.detectarTipo(conCabecera(PNG, 1000)));
        assertEquals("image/jpeg", LogoEmpresaService.detectarTipo(conCabecera(JPEG, 1000)));
    }

    @Test
    void testDeberiaRechazarLoQueNoEsPngNiJpegAunqueSeaPequeno() {
        byte[] gif = "GIF89a....".getBytes();
        byte[] pdf = "%PDF-1.7".getBytes();
        byte[] ejecutable = {'M', 'Z', 0, 0, 0};

        assertThrows(LogoInvalidoException.class, () -> LogoEmpresaService.detectarTipo(gif));
        assertThrows(LogoInvalidoException.class, () -> LogoEmpresaService.detectarTipo(pdf));
        assertThrows(LogoInvalidoException.class, () -> LogoEmpresaService.detectarTipo(ejecutable));
    }

    @Test
    void testDeberiaRechazarUnArchivoVacioONulo() {
        assertThrows(LogoInvalidoException.class, () -> LogoEmpresaService.detectarTipo(new byte[0]));
        assertThrows(LogoInvalidoException.class, () -> LogoEmpresaService.detectarTipo(null));
    }

    @Test
    void testDeberiaRechazarUnArchivoMasGrandeQueElTopeYAceptarUnoJustoEnElTope() {
        byte[] justo = conCabecera(PNG, LogoEmpresaService.MAXIMO_BYTES);
        byte[] pasado = conCabecera(PNG, LogoEmpresaService.MAXIMO_BYTES + 1);

        assertEquals("image/png", LogoEmpresaService.detectarTipo(justo));
        LogoInvalidoException e = assertThrows(LogoInvalidoException.class, () -> LogoEmpresaService.detectarTipo(pasado));
        assertEquals(true, e.getMessage().contains("máximo"));
    }

    @Test
    void testDeberiaNoReconocerComoPngUnaCabeceraIncompleta() {
        byte[] truncado = Arrays.copyOf(PNG, 4);
        assertThrows(LogoInvalidoException.class, () -> LogoEmpresaService.detectarTipo(truncado));
    }
}
