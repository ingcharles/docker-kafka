package com.facturacion.identidad.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class UsuarioServiceTest {

    @Test
    void testDeberiaNormalizarElCorreoSinEspaciosYEnMinusculas() {
        assertEquals("ana@x.com", UsuarioService.normalizarEmail("  Ana@X.COM "));
        assertEquals("ana@x.com", UsuarioService.normalizarEmail("ana@x.com"));
    }

    @Test
    void testDeberiaDejarNuloUnCorreoNulo() {
        assertNull(UsuarioService.normalizarEmail(null));
    }
}
