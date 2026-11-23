package com.facturacion.commons.correo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class ListaCorreosTest {

    @Test
    void testDeberiaPartirRecortaCadaPiezaYConservaLasVacias() {
        assertEquals(List.of("a@x.com", "", "b@x.com", ""), ListaCorreos.partir(" a@x.com , ,b@x.com,"));
    }

    @Test
    void testDeberiaTextoNuloOEnBlancoNoTieneCorreos() {
        assertTrue(ListaCorreos.partir(null).isEmpty());
        assertTrue(ListaCorreos.partir("   ").isEmpty());
        assertTrue(ListaCorreos.separar(null).isEmpty());
    }

    @Test
    void testDeberiaSepararDescartaLasPiezasVacias() {
        assertEquals(List.of("a@x.com", "b@x.com"), ListaCorreos.separar("a@x.com,, b@x.com,"));
    }

    @Test
    void testDeberiaUnirUsaElSeparador() {
        assertEquals("a@x.com,b@x.com", ListaCorreos.unir(List.of("a@x.com", "b@x.com")));
    }
}
