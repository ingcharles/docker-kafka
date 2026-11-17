package com.facturacion.notificacion.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.channels.ClosedChannelException;

import org.junit.jupiter.api.Test;

class MotivoFallaTest {

    @Test
    void unaExcepcionSinMensajeNoDaMotivoVacio() {
        // ClosedChannelException.getMessage() es null: antes eso se guardaba como "enviado sin error".
        String motivo = MotivoFalla.de(new ClosedChannelException());

        assertFalse(motivo.isBlank());
        assertEquals("ClosedChannelException", motivo);
    }

    @Test
    void incluyeElMensajeYLaCausaRaiz() {
        Exception falla = new RuntimeException("no se pudo enviar", new IllegalStateException("handshake cortado"));

        assertEquals("RuntimeException: no se pudo enviar (causa: IllegalStateException: handshake cortado)",
            MotivoFalla.de(falla));
    }

    @Test
    void sinFallaDevuelveUnTextoGenerico() {
        assertFalse(MotivoFalla.de(null).isBlank());
    }

    @Test
    void recortaAlTamanoDeLaColumna() {
        String motivo = MotivoFalla.de(new RuntimeException("x".repeat(2000)));

        assertEquals(MotivoFalla.LONGITUD_MAXIMA, motivo.length());
        assertTrue(motivo.startsWith("RuntimeException: xxx"));
    }
}
