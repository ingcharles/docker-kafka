package com.facturacion.notificacion.shared.error;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.facturacion.commons.error.Mensajes;

/** Garantiza que el enum de códigos de log y mensajes.properties no se desfasan. */
class CodigoLogTest {

    @Test
    void cadaCodigoTieneSuMensaje() {
        for (CodigoLog codigo : CodigoLog.values()) {
            assertTrue(Mensajes.existe(codigo.claveMensaje()), codigo + " no tiene clave en mensajes.properties");
            assertFalse(Mensajes.obtenerTexto(codigo, 1, 2).isBlank(), codigo + " tiene mensaje vacío");
        }
    }

    @Test
    void losCodigosSonUnicosYSiguenElFormatoDelCatalogo() {
        Set<String> vistos = new HashSet<>();
        for (CodigoLog codigo : CodigoLog.values()) {
            assertTrue(codigo.codigo().matches("[A-Z]{2,4}-[1-5][0-9]{2}"), codigo.codigo());
            assertTrue(vistos.add(codigo.codigo()), "código repetido " + codigo.codigo());
        }
    }

    @Test
    void mensajesNoUsadosNoSeAcumulan() {
        Set<String> usadas = Arrays.stream(CodigoLog.values()).map(CodigoLog::claveMensaje).collect(Collectors.toSet());
        Set<String> sobrantes = new HashSet<>(Mensajes.listarClaves());
        sobrantes.removeAll(usadas);
        assertTrue(sobrantes.isEmpty(), "Claves sin código: " + sobrantes);
    }
}
