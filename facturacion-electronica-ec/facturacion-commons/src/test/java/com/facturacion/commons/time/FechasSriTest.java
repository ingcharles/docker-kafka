package com.facturacion.commons.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.Test;

class FechasSriTest {

    /** 02:30 UTC del 18/11/2026 son las 21:30 del 17/11/2026 en Ecuador. */
    private static final Instant MADRUGADA_UTC = Instant.parse("2026-11-18T02:30:15Z");

    @Test
    void testDeberiaFormatearSiempreEnHoraDeEcuador() {
        assertEquals("17/11/2026", FechasSri.formatearFecha(MADRUGADA_UTC));
        assertEquals("17/11/2026 21:30:15", FechasSri.formatearFechaHora(MADRUGADA_UTC));
        assertEquals("17/11/2026 21:30", FechasSri.formatearFechaHoraCorta(MADRUGADA_UTC));
        assertEquals(LocalDate.of(2026, 11, 17), FechasSri.convertirAFecha(MADRUGADA_UTC));
    }

    @Test
    void testDeberiaFormatearLaFechaDeLaClaveDeAccesoSinSeparadores() {
        assertEquals("18112026", FechasSri.formatearFechaClaveAcceso(LocalDate.of(2026, 11, 18)));
        assertEquals("05012027", FechasSri.formatearFechaClaveAcceso(LocalDate.of(2027, 1, 5)));
        assertEquals("05/01/2027", FechasSri.formatearFecha(LocalDate.of(2027, 1, 5)));
    }

    @Test
    void testDeberiaLeerLasFechasDelSriDeFormaEstricta() {
        assertEquals(LocalDate.of(2026, 2, 28), FechasSri.leerFecha("28/02/2026"));
        assertThrows(DateTimeParseException.class, () -> FechasSri.leerFecha("31/02/2026"));
        assertThrows(DateTimeParseException.class, () -> FechasSri.leerFecha("2026-02-28"));
        assertThrows(DateTimeParseException.class, () -> FechasSri.leerFecha("texto"));
    }

    @Test
    void testDeberiaTomarLaMedianocheDeEcuadorComoInicioDelDia() {
        assertEquals(Instant.parse("2026-11-17T05:00:00Z"), FechasSri.obtenerInicioDelDia("17/11/2026"));
    }

    @Test
    void testDeberiaUsarUnDesfaseDeCincoHorasRespectoDeUtc() {
        assertEquals(-5 * 3600, ZonaHoraria.ECUADOR.getTotalSeconds());
    }
}
