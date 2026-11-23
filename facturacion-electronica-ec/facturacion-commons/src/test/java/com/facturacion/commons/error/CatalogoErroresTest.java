package com.facturacion.commons.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/** Reglas que debe cumplir toda entrada del catálogo común; los servicios aplican las mismas a su propio enum. */
class CatalogoErroresTest {

    private static final Pattern FORMATO = Pattern.compile("^[A-Z]+-(\\d{3})-\\d{3}$");

    @Test
    void testDeberiaExigirCodigosUnicosConFormatoYEstadoCoherente() {
        Set<String> vistos = new HashSet<>();
        for (ErrorComun error : ErrorComun.values()) {
            Matcher m = FORMATO.matcher(error.codigo());
            assertTrue(m.matches(), "Formato de código inválido: " + error.codigo());
            assertEquals(Integer.parseInt(m.group(1)), error.estadoHttp(),
                "El estado del código no coincide con estadoHttp: " + error);
            assertTrue(vistos.add(error.codigo()), "Código repetido: " + error.codigo());
        }
    }

    @Test
    void testDeberiaExigirTextoEnLosMensajesComunesParaCadaEntrada() {
        for (ErrorComun error : ErrorComun.values()) {
            assertTrue(Mensajes.existeComun(error.claveMensaje()), "Falta el texto de " + error);
        }
    }

    @Test
    void testDeberiaReemplazarLosParametrosDelMensajeSinReglasDeComillas() {
        assertEquals("No existe 'x'", Mensajes.obtenerTexto(ErrorComun.RECURSO_NO_ENCONTRADO, "No existe 'x'"));
        assertEquals("Falló la validación", Mensajes.obtenerTexto(ErrorComun.VALIDACION_FALLIDA));
    }
}
