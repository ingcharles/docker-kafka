package com.facturacion.commons.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.facturacion.commons.exception.SinPermisoException;

class TokenPrincipalTest {

    private static TokenPrincipal con(String ruc, String rol) {
        TokenPrincipal principal = new TokenPrincipal();
        principal.setRuc(ruc);
        principal.setRol(rol);
        return principal;
    }

    @Test
    void testDeberiaSoloEsDeLaEmpresaCuyoRucCoincide() {
        TokenPrincipal principal = con("1790000000001", "ADMIN");
        assertTrue(principal.esDeLaEmpresa("1790000000001"));
        assertFalse(principal.esDeLaEmpresa("1791111111001"));
        assertFalse(principal.esDeLaEmpresa(null));
    }

    @Test
    void testDeberiaUnTokenSinRucNoEsDeNingunaEmpresa() {
        assertFalse(con(null, "ADMIN").esDeLaEmpresa("1790000000001"));
        assertFalse(con(null, "ADMIN").esDeLaEmpresa(null));
    }

    @Test
    void testDeberiaAdminYOperadorPuedenEscribir() {
        assertDoesNotThrow(() -> con("1790000000001", "ADMIN").exigirEscritura());
        assertDoesNotThrow(() -> con("1790000000001", "OPERADOR").exigirEscritura());
    }

    @Test
    void testDeberiaLecturaYSinRolNoPuedenEscribir() {
        assertThrows(SinPermisoException.class, () -> con("1790000000001", "LECTURA").exigirEscritura());
        assertThrows(SinPermisoException.class, () -> con("1790000000001", null).exigirEscritura());
    }

    @Test
    void testDeberiaSoloAdminPasaLaExigenciaDeAdmin() {
        assertDoesNotThrow(() -> con("1790000000001", "ADMIN").exigirAdmin());
        assertThrows(SinPermisoException.class, () -> con("1790000000001", "OPERADOR").exigirAdmin());
        assertThrows(SinPermisoException.class, () -> con("1790000000001", "LECTURA").exigirAdmin());
        assertThrows(SinPermisoException.class, () -> con("1790000000001", null).exigirAdmin());
    }
}
