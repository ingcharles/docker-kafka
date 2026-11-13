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
import com.facturacion.identidad.shared.exception.CredencialesInvalidasException;
import com.facturacion.identidad.shared.exception.EmailDuplicadoException;
import com.facturacion.identidad.shared.exception.RolInsuficienteException;
import com.facturacion.identidad.shared.exception.SinAsociacionException;

/** Mantiene alineados el enum del catálogo y mensajes.properties, y los textos/estados que ya recibían los clientes. */
class IdentidadErrorTest {

    @Test
    void cadaCodigoTieneSuMensajeYNoSobranClaves() {
        for (IdentidadError error : IdentidadError.values()) {
            assertTrue(Mensajes.existe(error.claveMensaje()), error + " no tiene clave en mensajes.properties");
            assertFalse(Mensajes.de(error, "x").isBlank(), error + " tiene mensaje vacío");
        }
        Set<String> sobrantes = new HashSet<>(Mensajes.claves());
        sobrantes.removeAll(
            Arrays.stream(IdentidadError.values()).map(IdentidadError::claveMensaje).collect(Collectors.toSet()));
        assertTrue(sobrantes.isEmpty(), "Claves sin código de error: " + sobrantes);
    }

    @Test
    void codigosUnicosConFormatoYEstadoCoherente() {
        Set<String> vistos = new HashSet<>();
        for (IdentidadError error : IdentidadError.values()) {
            assertTrue(error.codigo().matches("^IDE-" + error.estadoHttp() + "-\\d{3}$"), error.codigo());
            assertTrue(vistos.add(error.codigo()), "Código repetido: " + error.codigo());
        }
    }

    @Test
    void lasExcepcionesConservanElTextoYElEstadoDeAntes() {
        assertEquals("Ya existe un usuario registrado con el email a@x.com",
            new EmailDuplicadoException("a@x.com").getMessage());
        assertEquals(409, new EmailDuplicadoException("a@x.com").getError().estadoHttp());

        assertEquals("Se requiere el rol ADMIN sobre el ruc 1790000000001 para gestionar sus miembros",
            new RolInsuficienteException("1790000000001").getMessage());
        assertEquals("La empresa 1790000000001 ya tiene administradores: pide a uno de ellos que te agregue "
                + "(POST /empresas/1790000000001/usuarios).",
            RolInsuficienteException.empresaYaTieneAdministradores("1790000000001").getMessage());
        assertEquals(403, RolInsuficienteException.empresaYaTieneAdministradores("1").getError().estadoHttp());

        assertEquals("El usuario no está asociado al ruc 1790000000001 — asóciate primero vía POST /usuarios/me/empresas",
            new SinAsociacionException("1790000000001").getMessage());

        CredencialesInvalidasException credenciales = new CredencialesInvalidasException("Email o contraseña incorrectos");
        assertEquals("Email o contraseña incorrectos", credenciales.getMessage());
        assertEquals(401, credenciales.getError().estadoHttp());
    }
}
