package com.facturacion.notificacion.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.channels.ClosedChannelException;

import org.junit.jupiter.api.Test;

class MotivoFallaTest {

    @Test
    void testDeberiaNoDevolverMotivoVacioParaUnaExcepcionSinMensaje() {
        // ClosedChannelException.getMessage() es null: antes eso se guardaba como "enviado sin error".
        String motivo = MotivoFalla.obtenerMotivo(new ClosedChannelException());

        assertFalse(motivo.isBlank());
        assertEquals("ClosedChannelException", motivo);
    }

    @Test
    void testDeberiaIncluirElMensajeYLaCausaRaiz() {
        Exception falla = new RuntimeException("no se pudo enviar", new IllegalStateException("handshake cortado"));

        assertEquals("RuntimeException: no se pudo enviar (causa: IllegalStateException: handshake cortado)",
            MotivoFalla.obtenerMotivo(falla));
    }

    @Test
    void testDeberiaDevolverUnTextoGenericoSiNoHayFalla() {
        assertFalse(MotivoFalla.obtenerMotivo(null).isBlank());
    }

    @Test
    void testDeberiaRecortarAlTamanoDeLaColumna() {
        String motivo = MotivoFalla.obtenerMotivo(new RuntimeException("x".repeat(2000)));

        assertEquals(MotivoFalla.LONGITUD_MAXIMA, motivo.length());
        assertTrue(motivo.startsWith("RuntimeException: xxx"));
    }
}
