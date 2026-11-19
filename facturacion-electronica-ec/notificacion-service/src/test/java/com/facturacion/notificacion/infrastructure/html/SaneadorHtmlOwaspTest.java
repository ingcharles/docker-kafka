package com.facturacion.notificacion.infrastructure.html;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Lo que una empresa puede subir como HTML de su correo: lo útil se conserva y lo peligroso se quita. */
class SaneadorHtmlOwaspTest {

    private final SaneadorHtmlOwasp saneador = new SaneadorHtmlOwasp();

    private String sanear(String html) {
        return saneador.sanear(html);
    }

    @Test
    void conservaTextoTablasListasYEstilosEnLinea() {
        String limpio = sanear("<table width=\"100%\" cellpadding=\"0\" style=\"background-color:#f3f4f6;padding:12px\">"
            + "<tr><td align=\"center\" style=\"color:#1f2937;font-size:15px\"><b>Hola</b> <i>mundo</i></td></tr></table>"
            + "<ul><li>uno</li></ul><h1>T</h1><hr><p style=\"margin:0\">x</p>");

        for (String esperado : new String[] {"<table", "width=\"100%\"", "background-color:#f3f4f6", "<td", "align=\"center\"",
            "<b>Hola</b>", "<ul>", "<li>uno</li>", "<h1>T</h1>", "<hr />", "<p style=\"margin:0\">"}) {
            assertTrue(limpio.contains(esperado), "falta " + esperado + " en " + limpio);
        }
    }

    @Test
    void conservaLasVariablesEnElTexto() {
        String limpio = sanear("<p>Hola {{nombreReceptor}}, su factura {{numero}}</p><div>{{logo}}</div>");

        assertTrue(limpio.contains("{{nombreReceptor}}"), limpio);
        assertTrue(limpio.contains("{{numero}}"), limpio);
        assertTrue(limpio.contains("<div>{{logo}}</div>"), limpio);
    }

    @Test
    void unTextoParecidoALaMarcaInternaNoSeConfundeConUnaVariable() {
        String limpio = sanear("<p>ZZVAR0ZZ y ZZVAR7ZZ {{numero}} ZZVAR1ZZ</p>");

        assertEquals("<p>ZZVAR0ZZ y ZZVAR7ZZ {{numero}} ZZVAR1ZZ</p>", limpio);
    }

    @Test
    void lasVariablesConEspaciosOSinNombreValidoTambienSeConservanParaQueElValidadorLasRechace() {
        String limpio = sanear("<p>{{ numero }} {{inventada}}</p>");

        assertTrue(limpio.contains("{{ numero }}") && limpio.contains("{{inventada}}"), limpio);
    }

    @Test
    void quitaScriptsConSuContenido() {
        String limpio = sanear("<p>a</p><script>alert(1)</script><SCRIPT SRC=\"https://x/y.js\"></SCRIPT><p>b</p>");

        assertFalse(limpio.toLowerCase().contains("script"));
        assertFalse(limpio.contains("alert"));
        assertTrue(limpio.contains("<p>a</p>") && limpio.contains("<p>b</p>"));
    }

    @Test
    void quitaLosManejadoresDeEventos() {
        String limpio = sanear("<p onclick=\"x()\" onmouseover=\"y()\">a</p><img src=\"https://x/a.png\" onerror=\"z()\">");

        assertFalse(limpio.contains("onclick"));
        assertFalse(limpio.contains("onmouseover"));
        assertFalse(limpio.contains("onerror"));
        assertTrue(limpio.contains("<p>a</p>"));
    }

    @Test
    void quitaFormulariosMarcosObjetosYHojasDeEstilo() {
        String limpio = sanear("<form action=\"https://x\"><input name=\"p\"><button>ok</button></form>"
            + "<iframe src=\"https://x\"></iframe><object data=\"x\"></object><embed src=\"x\">"
            + "<style>body{background:url(https://x/t.gif)}</style><link rel=\"stylesheet\" href=\"https://x/a.css\">"
            + "<svg onload=\"x()\"></svg><meta http-equiv=\"refresh\" content=\"0;url=https://x\"><base href=\"https://x\">");

        for (String peligroso : new String[] {"<form", "<input", "<button", "<iframe", "<object", "<embed", "<style", "<link",
            "<svg", "<meta", "<base"}) {
            assertFalse(limpio.toLowerCase().contains(peligroso), peligroso + " sigue en " + limpio);
        }
        assertFalse(limpio.contains("background:url"));
    }

    @Test
    void quitaLasDireccionesQueNoSonHttpsMailtoNiCid() {
        String limpio = sanear("<a href=\"javascript:alert(1)\">a</a><a href=\"data:text/html,x\">b</a>"
            + "<a href=\"http://x.test\">c</a><a href=\"vbscript:x\">d</a><a href=\"JaVaScRiPt:alert(1)\">e</a>"
            + "<img src=\"data:image/png;base64,AAAA\"><img src=\"http://x.test/a.png\"><img src=\"javascript:x\">");

        assertFalse(limpio.toLowerCase().contains("javascript"));
        assertFalse(limpio.contains("data:"));
        assertFalse(limpio.contains("http://"));
        assertFalse(limpio.toLowerCase().contains("vbscript"));
    }

    @Test
    void conservaEnlacesHttpsMailtoEImagenesHttpsYCid() {
        String limpio = sanear("<a href=\"https://ejemplo.test/ver\">ver</a> <a href=\"mailto:ayuda@ejemplo.test\">ayuda</a>"
            + "<img src=\"https://ejemplo.test/a.png\" alt=\"A\" width=\"120\"><img src=\"cid:logo@facturacion\" alt=\"Logo\">");

        assertTrue(limpio.contains("href=\"https://ejemplo.test/ver\""), limpio);
        assertTrue(limpio.contains("href=\"mailto:ayuda@ejemplo.test\""), limpio);
        assertTrue(limpio.contains("src=\"https://ejemplo.test/a.png\""), limpio);
        assertTrue(limpio.contains("src=\"cid:logo@facturacion\""), limpio);
    }

    @Test
    void losEnlacesNoPasanElReferenteNiAbrenLaVentanaDeOrigen() {
        String limpio = sanear("<a href=\"https://ejemplo.test\" target=\"_blank\">x</a>");

        assertTrue(limpio.contains("noopener"), limpio);
        assertTrue(limpio.contains("noreferrer"), limpio);
        assertFalse(limpio.contains("target="), limpio);
    }

    @Test
    void quitaEstilosPeligrososPeroDejaLosSeguros() {
        String limpio = sanear("<p style=\"color:red;background:url(javascript:alert(1));position:fixed;"
            + "behavior:url(x.htc);expression(alert(1));width:expression(alert(1))\">a</p>"
            + "<div style=\"background-image:url(https://x.test/t.gif)\">b</div>");

        assertTrue(limpio.contains("color:red"), limpio);
        assertFalse(limpio.toLowerCase().contains("javascript"), limpio);
        assertFalse(limpio.toLowerCase().contains("expression"), limpio);
        assertFalse(limpio.toLowerCase().contains("behavior"), limpio);
        assertFalse(limpio.contains("position"), limpio);
        assertFalse(limpio.contains("url("), limpio);
    }

    @Test
    void unDocumentoCompletoQuedaSoloConSuContenido() {
        String limpio = sanear("<!DOCTYPE html><html><head><title>t</title><meta charset=\"utf-8\"></head>"
            + "<body bgcolor=\"red\" onload=\"x()\"><p>Hola</p></body></html>");

        assertFalse(limpio.contains("<html"));
        assertFalse(limpio.contains("<head"));
        assertFalse(limpio.contains("<body"));
        assertFalse(limpio.contains("onload"));
        assertTrue(limpio.contains("<p>Hola</p>"));
    }

    @Test
    void unHtmlMalFormadoSeCorrige() {
        String limpio = sanear("<p>uno<b>dos<p>tres");

        assertTrue(limpio.contains("uno") && limpio.contains("dos") && limpio.contains("tres"));
        assertFalse(limpio.contains("<script"));
    }

    @Test
    void soloScriptsDejaElCuerpoVacio() {
        assertTrue(sanear("<script>alert(1)</script><style>p{}</style>").isBlank());
    }
}
