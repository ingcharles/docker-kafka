package com.facturacion.notificacion.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.facturacion.commons.exception.ApiException;
import com.facturacion.notificacion.application.dto.PlantillaCorreoRequest;
import com.facturacion.notificacion.application.dto.VariablePlantillaResponse;
import com.facturacion.notificacion.application.dto.VistaPreviaRequest;
import com.facturacion.notificacion.domain.model.enums.TipoComprobanteCorreo;
import com.facturacion.notificacion.domain.model.enums.VariableCorreo;
import com.facturacion.notificacion.domain.model.vo.ContenidoCorreo;
import com.facturacion.notificacion.domain.model.vo.PlantillaPorDefecto;
import com.facturacion.notificacion.domain.port.ArmadorContenidoCorreo;
import com.facturacion.notificacion.domain.port.CorreoSalida;
import com.facturacion.notificacion.domain.port.SaneadorHtml;
import com.facturacion.notificacion.infrastructure.html.SaneadorHtmlOwasp;
import com.facturacion.notificacion.shared.error.NotificacionError;

/**
 * Las reglas de guardar una plantilla se aplican antes de abrir ninguna sesión de base de datos: una plantilla inválida se
 * rechaza sin tocarla (por eso se prueban sin base).
 */
class PlantillaCorreoServiceTest {

    private static final String RUC = "1722039953001";

    private final ArmadorContenidoCorreo armador = new ArmadorContenidoCorreo() {
        @Override
        public ContenidoCorreo armar(CorreoSalida.Comprobante comprobante) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PlantillaPorDefecto exportarPorDefecto(TipoComprobanteCorreo tipo) {
            return new PlantillaPorDefecto("asunto " + tipo, "<p>html</p>", "texto");
        }
    };
    private final SaneadorHtml saneador = new SaneadorHtmlOwasp();
    private final PlantillaCorreoService servicio = new PlantillaCorreoService(null, saneador, armador, null);

    private ApiException rechazo(String tipo, String asunto, String html, String texto) {
        return assertThrows(ApiException.class,
            () -> servicio.guardar(RUC, tipo, new PlantillaCorreoRequest(asunto, html, texto, null)));
    }

    @Test
    void testDeberiaUnTipoQueNoExisteSeRechazaConLosTiposValidos() {
        for (String tipo : new String[] {"BOLETA", "factura", "", " FACTURA", "TODOS"}) {
            ApiException error = rechazo(tipo, "Asunto", "<p>Hola</p>", null);

            assertEquals(NotificacionError.PLANTILLA_TIPO_INVALIDO, error.getError(), tipo);
            assertTrue(error.getMessage().contains("NOTA_CREDITO") && error.getMessage().contains("GENERICO"), error.getMessage());
        }
    }

    @Test
    void testDeberiaSiElHtmlSoloTieneLoPeligrosoSeRechazaPorQuedarVacio() {
        for (String html : new String[] {"<script>alert(1)</script>", "<style>p{}</style><iframe src=\"https://x\"></iframe>"}) {
            assertEquals(NotificacionError.PLANTILLA_SIN_CONTENIDO, rechazo("FACTURA", "Asunto", html, null).getError(), html);
        }
    }

    @Test
    void testDeberiaLasReglasDeLasVariablesSeAplicanAntesDeGuardar() {
        assertEquals(NotificacionError.PLANTILLA_VARIABLE_DESCONOCIDA,
            rechazo("FACTURA", "{{nada}}", "<p>x</p>", null).getError());
        assertEquals(NotificacionError.PLANTILLA_VARIABLE_EN_ETIQUETA,
            rechazo("FACTURA", "Asunto", "<a href=\"{{claveAcceso}}\">x</a>", null).getError());
        assertEquals(NotificacionError.PLANTILLA_ASUNTO_MULTILINEA,
            rechazo("FACTURA", "a\nb", "<p>x</p>", null).getError());
    }

    @Test
    void testDeberiaLaVistaPreviaTambienRechazaLoQueNoSePodriaGuardar() {
        VistaPreviaRequest invalida = new VistaPreviaRequest("FACTURA", "{{nada}}", "<p>x</p>", null);

        ApiException error = assertThrows(ApiException.class, () -> servicio.armarVistaPrevia(RUC, invalida));

        assertEquals(NotificacionError.PLANTILLA_VARIABLE_DESCONOCIDA, error.getError());
        assertEquals(NotificacionError.PLANTILLA_TIPO_INVALIDO, assertThrows(ApiException.class,
            () -> servicio.armarVistaPrevia(RUC, new VistaPreviaRequest("BOLETA", "a", "<p>x</p>", null))).getError());
    }

    @Test
    void testDeberiaLaPlantillaPorDefectoSePideConElNombreExactoDelTipo() {
        assertEquals("asunto NOTA_DEBITO", servicio.obtenerPorDefecto("NOTA_DEBITO").asunto());
        assertEquals("NOTA_DEBITO", servicio.obtenerPorDefecto("NOTA_DEBITO").tipoComprobante());
        assertEquals(NotificacionError.PLANTILLA_TIPO_INVALIDO,
            assertThrows(ApiException.class, () -> servicio.obtenerPorDefecto("nota_debito")).getError());
    }

    @Test
    void testDeberiaLasVariablesSeListanTodasConSuMarcadorYDescripcion() {
        List<VariablePlantillaResponse> variables = servicio.listarVariables();

        assertEquals(VariableCorreo.values().length, variables.size());
        assertTrue(variables.stream().anyMatch(v -> v.marcador().equals("{{numero}}") && !v.descripcion().isBlank()));
        assertTrue(variables.stream().anyMatch(v -> v.marcador().equals("{{logo}}")));
    }
}
