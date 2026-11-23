package com.facturacion.orquestacion.shared.error;

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
    void testDeberiaExigirMensajeParaCadaCodigo() {
        for (CodigoError codigo : CodigoError.values()) {
            assertTrue(Mensajes.existe(codigo.claveMensaje()), codigo + " no tiene clave en mensajes.properties");
            assertFalse(Mensajes.obtenerTexto(codigo).isBlank(), codigo + " tiene mensaje vacío");
        }
    }

    @Test
    void testDeberiaNoAcumularMensajesSinUso() {
        Set<String> usadas = Arrays.stream(CodigoError.values()).map(CodigoError::claveMensaje)
            .collect(Collectors.toSet());
        Arrays.stream(OrquestacionError.values()).forEach(e -> usadas.add(e.claveMensaje()));
        Set<String> sobrantes = new HashSet<>(Mensajes.listarClaves());
        sobrantes.removeAll(usadas);
        assertTrue(sobrantes.isEmpty(), "Claves sin código de error: " + sobrantes);
    }

    @Test
    void testDeberiaReemplazarLosMarcadoresConLosParametros() {
        assertEquals("Reintento automático 2 de 3: sin respuesta en estado FIRMADO, se vuelve a publicar el último comando.",
            Mensajes.obtenerTexto(CodigoError.SAGA_REINTENTO_AUTOMATICO, 2, 3, "FIRMADO"));
    }
}
