package com.facturacion.notificacion.domain.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.facturacion.commons.exception.ApiException;
import com.facturacion.notificacion.shared.error.NotificacionError;

class PlantillaCorreoValidadorTest {

    private static ApiException rechazo(String asunto, String html, String texto) {
        return assertThrows(ApiException.class, () -> PlantillaCorreoValidador.validar(asunto, html, texto));
    }

    @Test
    void testDeberiaAceptarUnaPlantillaConVariablesPermitidas() {
        assertDoesNotThrow(() -> PlantillaCorreoValidador.validar("{{tipoComprobante}} {{numero}}",
            "<p>Hola {{nombreReceptor}}</p><div>{{logo}}</div><td>{{total}}</td>", "Hola {{nombreReceptor}} {{logo}}"));
        assertDoesNotThrow(() -> PlantillaCorreoValidador.validar("Su factura", "<p>Sin variables</p>", null));
    }

    @Test
    void testDeberiaRechazarUnaVariableQueNoExisteInformandoLasPermitidas() {
        for (String[] caso : new String[][] {
            {"{{inventada}}", "<p>x</p>", null},
            {"ok", "<p>{{otra}}</p>", null},
            {"ok", "<p>x</p>", "{{numero2}}"},
            {"{{numero.toString()}}", "<p>x</p>", null},
            {"{{}}", "<p>x</p>", null}}) {
            ApiException error = rechazo(caso[0], caso[1], caso[2]);

            assertEquals(NotificacionError.PLANTILLA_VARIABLE_DESCONOCIDA, error.getError());
            assertTrue(error.getMessage().contains("{{numero}}"));
        }
    }

    @Test
    void testDeberiaRechazarUnaVariableDentroDeUnaEtiqueta() {
        for (String html : new String[] {
            "<a href=\"{{claveAcceso}}\">ver</a>",
            "<img src=\"{{logo}}\">",
            "<p title=\"{{numero}}\">x</p>",
            "<td style=\"color:{{numero}}\">x</td>"}) {
            assertEquals(NotificacionError.PLANTILLA_VARIABLE_EN_ETIQUETA, rechazo("ok", html, null).getError(), html);
        }
    }

    @Test
    void testDeberiaExigirQueElAsuntoVayaEnUnaSolaLinea() {
        assertEquals(NotificacionError.PLANTILLA_ASUNTO_MULTILINEA, rechazo("a\nBcc: x@y.z", "<p>x</p>", null).getError());
        assertEquals(NotificacionError.PLANTILLA_ASUNTO_MULTILINEA, rechazo("a\rb", "<p>x</p>", null).getError());
    }

    @Test
    void testDeberiaRechazarLaVariableLogoEnElAsunto() {
        assertEquals(NotificacionError.PLANTILLA_LOGO_EN_ASUNTO, rechazo("{{logo}}", "<p>x</p>", null).getError());
    }
}
