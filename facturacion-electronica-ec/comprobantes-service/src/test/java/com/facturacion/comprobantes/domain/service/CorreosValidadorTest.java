package com.facturacion.comprobantes.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.facturacion.commons.exception.ErrorNegocioException;
import com.facturacion.comprobantes.shared.error.CodigoError;

class CorreosValidadorTest {

    private static final int MAXIMO = 3;
    private static final String CAMPO = "compradorCorreo";

    private static String validar(String texto) {
        return CorreosValidador.validarYNormalizar(texto, CAMPO, MAXIMO);
    }

    private static CodigoError rechazar(String texto) {
        return (CodigoError) assertThrows(ErrorNegocioException.class, () -> validar(texto)).getCodigo();
    }

    @Test
    void testDeberiaAceptarQueElCorreoSeaOpcional() {
        assertNull(validar(null));
        assertNull(validar("  "));
    }

    @Test
    void testDeberiaAceptarUnCorreoValido() {
        assertEquals("a@x.com", validar("a@x.com"));
    }

    @Test
    void testDeberiaRecortarYNormalizarVariosCorreos() {
        assertEquals("a@x.com,b@x.com,c@x.com", validar(" a@x.com , b@x.com,c@x.com "));
    }

    @Test
    void testDeberiaRechazarUnCorreoSinFormaValida() {
        assertEquals(CodigoError.CORREO_INVALIDO, rechazar("sin-arroba"));
        assertEquals(CodigoError.CORREO_INVALIDO, rechazar("a@x"));
        assertEquals(CodigoError.CORREO_INVALIDO, rechazar("a b@x.com"));
        assertEquals(CodigoError.CORREO_INVALIDO, rechazar("a@x.com;b@x.com"));
    }

    @Test
    void testDeberiaRechazarLasPiezasVaciasDeLaLista() {
        assertEquals(CodigoError.CORREO_INVALIDO, rechazar("a@x.com,,b@x.com"));
        assertEquals(CodigoError.CORREO_INVALIDO, rechazar("a@x.com,"));
    }

    @Test
    void testDeberiaRechazarMasCorreosQueElMaximo() {
        assertEquals(CodigoError.CORREOS_EXCEDEN_MAXIMO, rechazar("a@x.com,b@x.com,c@x.com,d@x.com"));
    }

    @Test
    void testDeberiaRechazarCorreosRepetidosSinImportarMayusculas() {
        assertEquals(CodigoError.CORREOS_REPETIDOS, rechazar("a@x.com,A@X.com"));
    }

    @Test
    void testDeberiaRechazarUnaListaMasLargaQueLaColumna() {
        String largo = "a".repeat(CorreosValidador.LONGITUD_MAXIMA / 2);
        assertEquals(CodigoError.CORREOS_MUY_LARGOS, rechazar(largo + "@x.com," + largo + "@y.com"));
    }
}
