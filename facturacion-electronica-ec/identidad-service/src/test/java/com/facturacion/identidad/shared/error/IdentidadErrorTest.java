package com.facturacion.identidad.shared.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.facturacion.commons.error.Mensajes;
import com.facturacion.commons.exception.ErrorNegocioException;
import com.facturacion.identidad.shared.exception.EmailDuplicadoException;
import com.facturacion.identidad.shared.exception.RolInsuficienteException;
import com.facturacion.identidad.shared.exception.SinAsociacionException;

/** Mantiene alineados el enum del catálogo y mensajes.properties, y los textos/estados que ya recibían los clientes. */
class IdentidadErrorTest {

    @Test
    void testDeberiaCadaCodigoTieneSuMensajeYNoSobranClaves() {
        for (IdentidadError error : IdentidadError.values()) {
            assertTrue(Mensajes.existe(error.claveMensaje()), error + " no tiene clave en mensajes.properties");
            assertFalse(Mensajes.obtenerTexto(error, "x").isBlank(), error + " tiene mensaje vacío");
        }
        Set<String> sobrantes = new HashSet<>(Mensajes.listarClaves());
        sobrantes.removeAll(
            Arrays.stream(IdentidadError.values()).map(IdentidadError::claveMensaje).collect(Collectors.toSet()));
        assertTrue(sobrantes.isEmpty(), "Claves sin código de error: " + sobrantes);
    }

    @Test
    void testDeberiaCodigosUnicosConFormatoYEstadoCoherente() {
        Set<String> vistos = new HashSet<>();
        for (IdentidadError error : IdentidadError.values()) {
            assertTrue(error.codigo().matches("^IDE-" + error.estadoHttp() + "-\\d{3}$"), error.codigo());
            assertTrue(vistos.add(error.codigo()), "Código repetido: " + error.codigo());
        }
    }

    @Test
    void testDeberiaLasExcepcionesConservanElTextoYElEstadoDeAntes() {
        assertEquals("Ya existe un usuario registrado con el email a@x.com",
            new EmailDuplicadoException("a@x.com").getMessage());
        assertEquals(409, new EmailDuplicadoException("a@x.com").getError().estadoHttp());

        assertEquals("Se requiere el rol ADMIN sobre el ruc 1790000000001 para gestionar sus miembros",
            new RolInsuficienteException("1790000000001").getMessage());

        assertEquals("El usuario no está asociado al ruc 1790000000001 — pide a un ADMIN de esa empresa que te agregue "
                + "(POST /empresas/1790000000001/usuarios)",
            new SinAsociacionException("1790000000001").getMessage());

        ErrorNegocioException credenciales = new ErrorNegocioException(IdentidadError.CREDENCIALES_INVALIDAS);
        assertEquals("Email o contraseña incorrectos", credenciales.getMessage());
        assertEquals(401, credenciales.getError().estadoHttp());
    }
}
