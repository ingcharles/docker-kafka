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
    void reconoceUnPngYUnJpegPorSuCabecera() {
        assertEquals("image/png", LogoEmpresaService.tipoDe(conCabecera(PNG, 1000)));
        assertEquals("image/jpeg", LogoEmpresaService.tipoDe(conCabecera(JPEG, 1000)));
    }

    @Test
    void rechazaLoQueNoEsPngNiJpegAunqueSeaPequeno() {
        byte[] gif = "GIF89a....".getBytes();
        byte[] pdf = "%PDF-1.7".getBytes();
        byte[] ejecutable = {'M', 'Z', 0, 0, 0};

        assertThrows(LogoInvalidoException.class, () -> LogoEmpresaService.tipoDe(gif));
        assertThrows(LogoInvalidoException.class, () -> LogoEmpresaService.tipoDe(pdf));
        assertThrows(LogoInvalidoException.class, () -> LogoEmpresaService.tipoDe(ejecutable));
    }

    @Test
    void rechazaUnArchivoVacioONulo() {
        assertThrows(LogoInvalidoException.class, () -> LogoEmpresaService.tipoDe(new byte[0]));
        assertThrows(LogoInvalidoException.class, () -> LogoEmpresaService.tipoDe(null));
    }

    @Test
    void rechazaUnArchivoMasGrandeQueElTopeYAceptaUnoJustoEnElTope() {
        byte[] justo = conCabecera(PNG, LogoEmpresaService.MAXIMO_BYTES);
        byte[] pasado = conCabecera(PNG, LogoEmpresaService.MAXIMO_BYTES + 1);

        assertEquals("image/png", LogoEmpresaService.tipoDe(justo));
        LogoInvalidoException e = assertThrows(LogoInvalidoException.class, () -> LogoEmpresaService.tipoDe(pasado));
        assertEquals(true, e.getMessage().contains("máximo"));
    }

    @Test
    void unaCabeceraIncompletaNoEsUnPng() {
        byte[] truncado = Arrays.copyOf(PNG, 4);
        assertThrows(LogoInvalidoException.class, () -> LogoEmpresaService.tipoDe(truncado));
    }
}
