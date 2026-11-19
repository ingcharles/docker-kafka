package com.facturacion.notificacion.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HtmlATextoTest {

    @Test
    void losBloquesPasanALineasYLasCeldasAEspacios() {
        assertEquals("Título\nHola mundo\nNúmero 001\nTotal 11.50", HtmlATexto.convertir(
            "<h1>Título</h1><p>Hola <b>mundo</b></p><table><tr><td>Número</td><td>001</td></tr>"
                + "<tr><td>Total</td><td>11.50</td></tr></table>"));
    }

    @Test
    void losSaltosDeLineaSeRespetan() {
        assertEquals("uno\ndos", HtmlATexto.convertir("uno<br>dos"));
        assertEquals("uno\ndos", HtmlATexto.convertir("uno<br/>dos"));
        assertEquals("uno\ndos", HtmlATexto.convertir("uno<BR />dos"));
    }

    @Test
    void lasEntidadesSeConviertenEnCaracteres() {
        assertEquals("A & B <c> \"d\" 'e' @ + = ñ", HtmlATexto.convertir(
            "A &amp; B &lt;c&gt; &quot;d&quot; &#39;e&#39; &#64; &#43; &#61; &#xF1;"));
        assertEquals("a b", HtmlATexto.convertir("a&nbsp;b"));
    }

    @Test
    void unAmpersandEscapadoDosVecesNoSeVuelveEtiqueta() {
        assertEquals("&lt;", HtmlATexto.convertir("&amp;lt;"));
    }

    @Test
    void quitaLasEtiquetasYLosEspaciosSobrantes() {
        assertEquals("a b\n\nc", HtmlATexto.convertir("<div>  a   <span style=\"x\">b</span>  </div>\n\n\n\n<p>c</p>"));
    }

    @Test
    void unaEntidadNumericaInvalidaSeIgnora() {
        assertEquals("ab", HtmlATexto.convertir("a&#99999999999;b"));
    }
}
