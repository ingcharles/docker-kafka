package com.facturacion.commons.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class RevocacionApiKeysTest {

    private final RevocacionApiKeys revocaciones = new RevocacionApiKeys();

    @Test
    void testDeberiaRecordarLaRevocacionHastaQueVenzaElToken() {
        revocaciones.revocar("a", Instant.now().plusSeconds(60));
        assertTrue(revocaciones.estaRevocada("a"));
        assertFalse(revocaciones.estaRevocada("b"));
        assertFalse(revocaciones.estaRevocada(null));
    }

    @Test
    void testDeberiaNoGuardarTokensQueYaVencieron() {
        revocaciones.revocar("vieja", Instant.now().minusSeconds(5));
        assertFalse(revocaciones.estaRevocada("vieja"));
        assertEquals(0, revocaciones.contarRevocadas());
    }

    @Test
    void testDeberiaLimpiarLasRevocacionesVencidasAlRevocarOtra() throws InterruptedException {
        revocaciones.revocar("corta", Instant.now().plusMillis(50));
        Thread.sleep(80);
        revocaciones.revocar("nueva", Instant.now().plusSeconds(60));
        assertEquals(1, revocaciones.contarRevocadas());
    }

    @Test
    void testDeberiaConvertirElMensajeDeRevocacionAJsonYDeVuelta() {
        Instant vence = Instant.ofEpochSecond(1_900_000_000L);
        MensajeRevocacion original = new MensajeRevocacion("c0ffee-1234", vence);
        assertEquals("c0ffee-1234|1900000000", original.serializar());
        assertEquals(original, MensajeRevocacion.parsear(original.serializar()));
    }

    @Test
    void testDeberiaRechazarUnMensajeDeRevocacionMalFormado() {
        assertThrows(IllegalArgumentException.class, () -> MensajeRevocacion.parsear(null));
        assertThrows(IllegalArgumentException.class, () -> MensajeRevocacion.parsear("sin-separador"));
        assertThrows(IllegalArgumentException.class, () -> MensajeRevocacion.parsear("|123"));
        assertThrows(IllegalArgumentException.class, () -> MensajeRevocacion.parsear("jti|abc"));
    }
}
