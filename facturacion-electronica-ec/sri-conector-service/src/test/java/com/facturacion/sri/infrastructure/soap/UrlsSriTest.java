package com.facturacion.sri.infrastructure.soap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class UrlsSriTest {

    private static final List<String> SOLO_SRI = List.of("*.sri.gob.ec");
    private static final String OFICIAL = "https://celcer.sri.gob.ec/ws";

    @Test
    void testDeberiaUsarLaUrlOficialSiNoHayOverride() {
        assertEquals(OFICIAL, UrlsSri.elegir(null, OFICIAL, SOLO_SRI));
        assertEquals(OFICIAL, UrlsSri.elegir("  ", OFICIAL, SOLO_SRI));
    }

    @Test
    void testDeberiaRespetarUnHostDelSriPorHttps() {
        String propia = "https://cel.sri.gob.ec/comprobantes-electronicos-ws/Recepcion";
        assertEquals(propia, UrlsSri.elegir(propia, OFICIAL, SOLO_SRI));
    }

    @Test
    void testDeberiaIgnorarHostsAjenosRedInternaMetadatosYEsquemasQueNoSonHttps() {
        for (String malo : List.of("https://atacante.example.com/x", "http://celcer.sri.gob.ec/ws",
            "https://169.254.169.254/latest/meta-data", "http://localhost:8080/x", "https://postgres:5432",
            "https://sri.gob.ec.atacante.com/x", "https://user:pass@celcer.sri.gob.ec/x", "file:///etc/passwd",
            "no es una url")) {
            assertFalse(UrlsSri.esPermitida(malo, SOLO_SRI), malo);
            assertEquals(OFICIAL, UrlsSri.elegir(malo, OFICIAL, SOLO_SRI), malo);
        }
    }

    @Test
    void testDeberiaCoincidirElDominioYSusSubdominiosConElPatronComodin() {
        assertTrue(UrlsSri.esPermitida("https://sri.gob.ec/x", SOLO_SRI));
        assertTrue(UrlsSri.esPermitida("https://a.b.sri.gob.ec/x", SOLO_SRI));
        assertFalse(UrlsSri.esPermitida("https://notsri.gob.ec/x", SOLO_SRI));
    }

    @Test
    void testDeberiaAceptarUnMockSoloConElComodinTotalDeDesarrollo() {
        assertTrue(UrlsSri.esPermitida("http://mock-sri:8080/x", List.of("*")));
    }
}
