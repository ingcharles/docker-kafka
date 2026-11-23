package com.facturacion.notificacion.infrastructure.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.facturacion.notificacion.domain.model.entity.PlantillaCorreoEmpresa;
import com.facturacion.notificacion.domain.model.enums.TipoComprobanteCorreo;
import com.facturacion.notificacion.domain.model.vo.ContenidoCorreo;
import com.facturacion.notificacion.domain.model.vo.LogoEmpresa;
import com.facturacion.notificacion.domain.model.vo.PlantillaPorDefecto;
import com.facturacion.notificacion.domain.port.CorreoSalida;
import com.facturacion.notificacion.domain.service.PlantillaCorreoValidador;
import com.facturacion.notificacion.infrastructure.html.SaneadorHtmlOwasp;

class PlantillaCorreoTest {

    // 09112026 | 01 | 1722039953001 | 1 | 001001 | 000000001 | 99102398 | 1 | 4
    private static final String CLAVE = "0911202601172203995300110010010000000019910239814";
    private static final String NUMERO = "001-001-000000001";
    private static final LogoEmpresa LOGO = new LogoEmpresa(new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 1}, "image/png");

    private final PlantillaCorreo plantilla = new PlantillaCorreo(MotorPlantillasDePrueba.crear());

    private static CorreoSalida.Ajustes ajustes(String asunto, String nombreEmisor, LogoEmpresa logo,
            PlantillaCorreoEmpresa propia) {
        return new CorreoSalida.Ajustes(asunto, null, nombreEmisor, logo, propia);
    }

    private static CorreoSalida.Comprobante comprobante(String tipo, String nombreReceptor, String claveAcceso,
            CorreoSalida.Ajustes ajustes) {
        return new CorreoSalida.Comprobante("receptor@ejemplo.test", claveAcceso, tipo, nombreReceptor,
            new BigDecimal("11.5"), new byte[] {1}, "<autorizacion/>", ajustes);
    }

    private static CorreoSalida.Comprobante comprobante(String tipo, String nombreReceptor, String nombreEmisor) {
        return comprobante(tipo, nombreReceptor, CLAVE, ajustes(null, nombreEmisor, null, null));
    }

    private static PlantillaCorreoEmpresa propia(String asunto, String html, String texto) {
        return PlantillaCorreoEmpresa.crear("1722039953001", TipoComprobanteCorreo.FACTURA, asunto, html, texto, true,
            Instant.now());
    }

    // ---- plantilla por defecto (archivos) ----

    @Test
    void testDeberiaArmarParaCadaTipoUnaPlantillaHtmlYTextoConLosDatosDelComprobante() {
        for (TipoComprobanteCorreo tipo : TipoComprobanteCorreo.values()) {
            ContenidoCorreo contenido = plantilla.armar(comprobante(tipo.name(), "Ana Pérez", null));

            for (String cuerpo : new String[] {contenido.html(), contenido.texto()}) {
                assertTrue(cuerpo.contains(NUMERO), tipo + ": número");
                assertTrue(cuerpo.contains("09/11/2026"), tipo + ": fecha de emisión");
                assertTrue(cuerpo.contains("11.50"), tipo + ": total");
                assertTrue(cuerpo.contains(CLAVE), tipo + ": clave de acceso");
                assertTrue(cuerpo.contains("1722039953001"), tipo + ": RUC del emisor");
                assertTrue(cuerpo.contains("Ana Pérez"), tipo + ": saludo");
                assertTrue(cuerpo.contains(tipo.etiqueta()), tipo + ": etiqueta");
                assertFalse(cuerpo.contains("{"), tipo + ": quedó una variable sin resolver");
            }
            assertEquals(tipo.etiqueta() + " " + NUMERO, contenido.asunto());
        }
    }

    @Test
    void testDeberiaEntregarElHtmlComoDocumentoCompleto() {
        String html = plantilla.armar(comprobante("FACTURA", "Ana", null)).html();

        assertTrue(html.startsWith("<!DOCTYPE html>"));
        assertTrue(html.contains("<title>Factura " + NUMERO + "</title>"));
        assertTrue(html.contains("<body"));
        assertTrue(html.contains("</html>"));
    }

    @Test
    void testDeberiaSepararConLineasEnBlancoElSaludoLaIntroduccionYLosDatosDelTexto() {
        String texto = plantilla.armar(comprobante("FACTURA", "Ana", null)).texto();

        assertTrue(texto.contains("Estimado(a) Ana,\n\nSe emitió"), texto);
        assertTrue(texto.contains("abajo.\n\nNúmero: " + NUMERO + "\nFecha de emisión: 09/11/2026\nTotal: USD 11.50\n"), texto);
        assertTrue(texto.contains("Clave de acceso: " + CLAVE + "\n\nAdjuntamos"), texto);
    }

    @Test
    void testDeberiaEscaparLosValoresEnElHtmlYNoEnElTexto() {
        ContenidoCorreo contenido = plantilla.armar(comprobante("FACTURA", "<script>alert(1)</script> & Cía", null));

        assertFalse(contenido.html().contains("<script>"));
        assertTrue(contenido.html().contains("&lt;script&gt;"));
        assertTrue(contenido.html().contains("&amp;"));
        assertTrue(contenido.texto().contains("<script>alert(1)</script> & Cía"));
    }

    @Test
    void testDeberiaSaludarEnGeneralSiNoHayNombreDelReceptor() {
        for (String nombre : new String[] {null, "", "   "}) {
            ContenidoCorreo contenido = plantilla.armar(comprobante("FACTURA", nombre, null));

            assertTrue(contenido.html().contains("Estimado(a) cliente,"));
            assertTrue(contenido.texto().contains("Estimado(a) cliente,"));
        }
    }

    @Test
    void testDeberiaAgregarElNombreDeLaEmpresaAlAsuntoAlEncabezadoYAlPie() {
        ContenidoCorreo contenido = plantilla.armar(comprobante("NOTA_CREDITO", null, "Tienda Ejemplo"));

        assertEquals("Nota de crédito " + NUMERO + " - Tienda Ejemplo", contenido.asunto());
        assertTrue(contenido.html().contains("Enviado por Tienda Ejemplo."));
        assertTrue(contenido.texto().contains("Enviado por Tienda Ejemplo."));
    }

    @Test
    void testDeberiaUsarLaPlantillaGenericaParaUnTipoDesconocido() {
        ContenidoCorreo contenido = plantilla.armar(comprobante("TIPO_NUEVO", null, null));

        assertEquals("Comprobante " + NUMERO, contenido.asunto());
        assertTrue(contenido.texto().contains("Se emitió a su nombre un comprobante electrónico"));
    }

    @Test
    void testDeberiaArmarElCorreoAunqueLaClaveNoTengaFormatoValido() {
        ContenidoCorreo contenido = plantilla.armar(comprobante("FACTURA", null, "123", ajustes(null, null, null, null)));

        assertEquals("Factura", contenido.asunto());
        assertTrue(contenido.texto().contains("Clave de acceso: 123"));
        assertFalse(contenido.texto().contains("Número:"));
        assertFalse(contenido.texto().contains("Fecha de emisión:"));
    }

    @Test
    void testDeberiaOmitirLaFilaDelTotalSiNoHayTotal() {
        CorreoSalida.Comprobante sinTotal = new CorreoSalida.Comprobante("receptor@ejemplo.test", CLAVE, "FACTURA", null,
            null, new byte[] {1}, "<autorizacion/>", ajustes(null, null, null, null));

        ContenidoCorreo contenido = plantilla.armar(sinTotal);

        assertFalse(contenido.html().contains("USD"));
        assertFalse(contenido.texto().contains("Total:"));
    }

    @Test
    void testDeberiaQuitarLosSaltosDeLineaDelAsunto() {
        ContenidoCorreo contenido = plantilla.armar(comprobante("FACTURA", null, "Tienda\r\nBcc: otro@ejemplo.test"));
        ContenidoCorreo conAjuste = plantilla.armar(comprobante("FACTURA", null, CLAVE,
            ajustes("Su factura\r\nBcc: otro@ejemplo.test", null, null, null)));

        for (String asunto : new String[] {contenido.asunto(), conAjuste.asunto()}) {
            assertFalse(asunto.contains("\n"));
            assertFalse(asunto.contains("\r"));
        }
    }

    @Test
    void testDeberiaOmitirLasEtiquetasHtmlEnElTexto() {
        for (TipoComprobanteCorreo tipo : TipoComprobanteCorreo.values()) {
            String texto = plantilla.armar(comprobante(tipo.name(), "Ana", null)).texto();

            assertFalse(texto.contains("<"), tipo.name());
        }
    }

    @Test
    void testDeberiaReemplazarElAsuntoPorDefectoConElAjusteDeAsuntoDeLaEmpresa() {
        ContenidoCorreo contenido = plantilla.armar(comprobante("FACTURA", null, CLAVE,
            ajustes("Su factura de Tienda", "Tienda", null, null)));

        assertEquals("Su factura de Tienda", contenido.asunto());
    }

    // ---- el logo ----

    @Test
    void testDeberiaPonerLaImagenIncrustadaEnElEncabezadoSiHayLogo() {
        ContenidoCorreo contenido = plantilla.armar(comprobante("FACTURA", null, CLAVE,
            ajustes(null, "Tienda Ejemplo", LOGO, null)));

        assertTrue(contenido.html().contains("<img src=\"cid:" + LogoEmpresa.CID + "\""), contenido.html());
        assertTrue(contenido.html().contains("alt=\"Tienda Ejemplo\""));
    }

    @Test
    void testDeberiaMostrarElNombreDeLaEmpresaEnElEncabezadoSinLogo() {
        ContenidoCorreo contenido = plantilla.armar(comprobante("FACTURA", null, "Tienda Ejemplo"));

        assertFalse(contenido.html().contains("<img"));
        assertFalse(contenido.html().contains("cid:"));
        assertTrue(contenido.html().contains("<span style=\"font-size:15px;font-weight:bold;\">Tienda Ejemplo</span>"));
    }

    @Test
    void testDeberiaMostrarSoloElTipoEnElEncabezadoSinLogoNiNombre() {
        ContenidoCorreo contenido = plantilla.armar(comprobante("FACTURA", null, null));

        assertFalse(contenido.html().contains("<img"));
        assertTrue(contenido.html().contains("Factura"));
    }

    // ---- plantilla propia de la empresa ----

    @Test
    void testDeberiaReemplazarLasVariablesYEscaparElHtmlEnLaPlantillaDeLaEmpresa() {
        PlantillaCorreoEmpresa propia = propia("{{tipoComprobante}} {{numero}} de {{nombreEmisor}}",
            "<p>Hola {{nombreReceptor}}, su {{tipoComprobante}} {{numero}} por USD {{total}} del {{fechaEmision}}.</p>"
                + "<p>{{claveAcceso}} / {{rucEmisor}}</p>",
            null);

        ContenidoCorreo contenido = plantilla.armar(comprobante("FACTURA", "Pérez & <Cía>", CLAVE,
            ajustes(null, "Tienda", null, propia)));

        assertEquals("Factura " + NUMERO + " de Tienda", contenido.asunto());
        assertTrue(contenido.html().contains("<p>Hola Pérez &amp; &lt;Cía&gt;, su Factura " + NUMERO
            + " por USD 11.50 del 09/11/2026.</p>"), contenido.html());
        assertTrue(contenido.html().contains(CLAVE + " / 1722039953001"));
        assertTrue(contenido.html().startsWith("<!DOCTYPE html>"));
    }

    @Test
    void testDeberiaDerivarElTextoDelHtmlSiLaPlantillaNoTraeTextoPropio() {
        PlantillaCorreoEmpresa propia = propia("Asunto", "<h1>Hola</h1><p>Su factura {{numero}}</p>"
            + "<table><tr><td>Total</td><td>{{total}}</td></tr></table>", null);

        ContenidoCorreo contenido = plantilla.armar(comprobante("FACTURA", null, CLAVE, ajustes(null, null, null, propia)));

        assertEquals("Hola\nSu factura " + NUMERO + "\nTotal 11.50", contenido.texto());
    }

    @Test
    void testDeberiaUsarLasVariablesSinEscaparEnElTextoPropio() {
        PlantillaCorreoEmpresa propia = propia("Asunto", "<p>Hola</p>", "Hola {{nombreReceptor}}: {{numero}}");

        ContenidoCorreo contenido = plantilla.armar(comprobante("FACTURA", "Pérez & Cía", CLAVE,
            ajustes(null, null, null, propia)));

        assertEquals("Hola Pérez & Cía: " + NUMERO, contenido.texto());
    }

    @Test
    void testDeberiaLlamarClienteAlReceptorSinNombreEnLaPlantillaDeEmpresa() {
        PlantillaCorreoEmpresa propia = propia("Asunto", "<p>Estimado(a) {{nombreReceptor}}</p>", null);

        ContenidoCorreo contenido = plantilla.armar(comprobante("FACTURA", null, CLAVE, ajustes(null, null, null, propia)));

        assertTrue(contenido.html().contains("<p>Estimado(a) cliente</p>"));
    }

    @Test
    void testDeberiaPriorizarLaPlantillaDeLaEmpresaSobreElAjusteDeAsunto() {
        PlantillaCorreoEmpresa propia = propia("Asunto de la plantilla", "<p>Hola</p>", null);

        ContenidoCorreo contenido = plantilla.armar(comprobante("FACTURA", null, CLAVE,
            ajustes("Asunto del ajuste", null, null, propia)));

        assertEquals("Asunto de la plantilla", contenido.asunto());
    }

    @Test
    void testDeberiaSustituirLaVariableLogoPorLaImagenOPorElNombreDeLaEmpresa() {
        PlantillaCorreoEmpresa propia = propia("Asunto", "<div>{{logo}}</div><p>Hola</p>", null);

        String conLogo = plantilla.armar(comprobante("FACTURA", null, CLAVE, ajustes(null, "Tienda", LOGO, propia))).html();
        String sinLogo = plantilla.armar(comprobante("FACTURA", null, CLAVE, ajustes(null, "Tienda", null, propia))).html();
        String sinNada = plantilla.armar(comprobante("FACTURA", null, CLAVE, ajustes(null, null, null, propia))).html();

        assertTrue(conLogo.contains("<img src=\"cid:" + LogoEmpresa.CID + "\""));
        assertFalse(sinLogo.contains("<img"));
        assertTrue(sinLogo.contains(">Tienda</span>"));
        assertTrue(sinNada.contains("<div></div>"));
    }

    @Test
    void testDeberiaSustituirLaVariableLogoPorElNombreDeLaEmpresaEnElTexto() {
        PlantillaCorreoEmpresa propia = propia("Asunto", "<p>x</p>", "{{logo}} - {{numero}}");

        ContenidoCorreo contenido = plantilla.armar(comprobante("FACTURA", null, CLAVE, ajustes(null, "Tienda", LOGO, propia)));

        assertEquals("Tienda - " + NUMERO, contenido.texto());
    }

    @Test
    void testDeberiaArmarElCorreoConLaPlantillaPorDefectoSiLaDeLaEmpresaFalla() {
        PlantillaCorreoEmpresa defectuosa = propia(null, "<p>Hola</p>", null);

        ContenidoCorreo contenido = plantilla.armar(comprobante("FACTURA", "Ana", CLAVE, ajustes(null, null, null, defectuosa)));

        assertEquals("Factura " + NUMERO, contenido.asunto());
        assertTrue(contenido.html().contains("Documento electrónico autorizado por el SRI"));
    }

    // ---- la plantilla por defecto como punto de partida ----

    @Test
    void testDeberiaExportarLaPlantillaPorDefectoConSusVariables() {
        PlantillaPorDefecto exportada = plantilla.exportarPorDefecto(TipoComprobanteCorreo.FACTURA);

        assertEquals("{{tipoComprobante}} {{numero}} - {{nombreEmisor}}", exportada.asunto());
        for (String variable : new String[] {"{{numero}}", "{{fechaEmision}}", "{{total}}", "{{claveAcceso}}", "{{rucEmisor}}",
            "{{nombreReceptor}}", "{{nombreEmisor}}", "{{tipoComprobante}}"}) {
            assertTrue(exportada.cuerpoHtml().contains(variable), "HTML sin " + variable);
            assertTrue(exportada.cuerpoTexto().contains(variable), "texto sin " + variable);
        }
        assertTrue(exportada.cuerpoHtml().contains("{{logo}}"));
        assertFalse(exportada.cuerpoHtml().contains("<html"));
        assertFalse(exportada.cuerpoHtml().contains("{#"));
        assertFalse(exportada.cuerpoTexto().contains("{#"));
    }

    @Test
    void testDeberiaGenerarUnaPlantillaValidaYIgualALaOriginalAlExportarLaPorDefecto() {
        for (TipoComprobanteCorreo tipo : TipoComprobanteCorreo.values()) {
            PlantillaPorDefecto exportada = plantilla.exportarPorDefecto(tipo);

            PlantillaCorreoValidador.validar(exportada.asunto(), exportada.cuerpoHtml(), exportada.cuerpoTexto());
            String limpio = new SaneadorHtmlOwasp().sanear(exportada.cuerpoHtml());
            PlantillaCorreoEmpresa guardada = PlantillaCorreoEmpresa.crear("1722039953001", tipo, exportada.asunto(), limpio,
                exportada.cuerpoTexto(), true, Instant.now());
            ContenidoCorreo conLaPropia = plantilla.armar(comprobante(tipo.name(), "Ana", CLAVE,
                ajustes(null, "Tienda", LOGO, guardada)));
            ContenidoCorreo conLaDeArchivo = plantilla.armar(comprobante(tipo.name(), "Ana", CLAVE,
                ajustes(null, "Tienda", LOGO, null)));

            assertEquals(conLaDeArchivo.asunto(), conLaPropia.asunto(), tipo.name());
            assertEquals(conLaDeArchivo.texto().strip(), conLaPropia.texto().strip(), tipo.name());
            for (String dato : new String[] {NUMERO, "09/11/2026", "11.50", CLAVE, "Ana", "cid:" + LogoEmpresa.CID}) {
                assertTrue(conLaPropia.html().contains(dato), tipo + ": falta " + dato);
            }
        }
    }
}
