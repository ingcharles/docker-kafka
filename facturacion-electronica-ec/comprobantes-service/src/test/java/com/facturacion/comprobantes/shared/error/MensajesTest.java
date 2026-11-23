package com.facturacion.comprobantes.shared.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.facturacion.commons.error.Mensajes;

/** Garantiza que enum de códigos y mensajes.properties no se desfasan. */
class MensajesTest {

    @Test
    void testDeberiaCadaCodigoTieneSuMensaje() {
        for (CodigoError codigo : CodigoError.values()) {
            assertTrue(Mensajes.existe(codigo.claveMensaje()), codigo + " no tiene clave en mensajes.properties");
            assertFalse(Mensajes.obtenerTexto(codigo).isBlank(), codigo + " tiene mensaje vacío");
        }
    }

    @Test
    void testDeberiaCadaCodigoDeLogTieneSuMensaje() {
        for (CodigoLog codigo : CodigoLog.values()) {
            assertTrue(Mensajes.existe(codigo.claveMensaje()), codigo + " no tiene clave en mensajes.properties");
            assertFalse(Mensajes.obtenerTexto(codigo, 1, 2, 3, 4).isBlank(), codigo + " tiene mensaje vacío");
        }
    }

    @Test
    void testDeberiaMensajesNoUsadosNoSeAcumulan() {
        Set<String> usadas = Arrays.stream(CodigoError.values()).map(CodigoError::claveMensaje)
            .collect(Collectors.toSet());
        Arrays.stream(CodigoLog.values()).forEach(log -> usadas.add(log.claveMensaje()));
        Set<String> sobrantes = new HashSet<>(Mensajes.listarClaves());
        sobrantes.removeAll(usadas);
        assertTrue(sobrantes.isEmpty(), "Claves sin código de error: " + sobrantes);
    }

    @Test
    void testDeberiaCodigosYClavesSonUnicos() {
        assertEquals(CodigoError.values().length,
            Arrays.stream(CodigoError.values()).map(CodigoError::codigo).distinct().count(), "códigos repetidos");
        assertEquals(CodigoError.values().length,
            Arrays.stream(CodigoError.values()).map(CodigoError::claveMensaje).distinct().count(),
            "claves repetidas");
    }

    @Test
    void testDeberiaLosParametrosReemplazanLosMarcadores() {
        assertEquals("El impuesto '9' no existe en la Tabla 16 del SRI. Códigos válidos: [2, 3].",
            Mensajes.obtenerTexto(CodigoError.IMPUESTO_NO_EXISTE, "9", java.util.List.of("2", "3")));
    }
}
