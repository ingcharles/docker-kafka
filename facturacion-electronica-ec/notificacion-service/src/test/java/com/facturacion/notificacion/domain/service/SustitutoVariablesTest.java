package com.facturacion.notificacion.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.facturacion.notificacion.domain.model.enums.VariableCorreo;

class SustitutoVariablesTest {

    @Test
    void testDeberiaReemplazarCadaVariablePorSuValor() {
        String resultado = SustitutoVariables.sustituir("{{tipoComprobante}} {{numero}} / {{numero}}",
            variable -> variable == VariableCorreo.NUMERO ? "001-001-000000001" : "Factura");

        assertEquals("Factura 001-001-000000001 / 001-001-000000001", resultado);
    }

    @Test
    void testDeberiaIgnorarLosEspaciosDentroDeLasLlaves() {
        assertEquals("1-2", SustitutoVariables.sustituir("{{ numero }}-{{  total}}", variable -> variable == VariableCorreo.NUMERO ? "1" : "2"));
    }

    @Test
    void testDeberiaReemplazarPorVacioUnaVariableQueNoExiste() {
        assertEquals("a--b", SustitutoVariables.sustituir("a-{{inventada}}-b", variable -> "x"));
    }

    @Test
    void testDeberiaInsertarElValorTalCualAunqueTengaSimbolosDeReemplazo() {
        assertEquals("pagó $1 y \\2", SustitutoVariables.sustituir("pagó {{total}}", variable -> "$1 y \\2"));
    }

    @Test
    void testDeberiaTratarUnValorNuloComoVacio() {
        assertEquals("[]", SustitutoVariables.sustituir("[{{numero}}]", variable -> null));
    }

    @Test
    void testDeberiaNoEvaluarUnaVariableComoExpresion() {
        assertEquals("[]", SustitutoVariables.sustituir("[{{numero.toString()}}]", variable -> "x"));
    }

    @Test
    void testDeberiaListarLosNombresDeLasVariablesQueAparecen() {
        assertEquals(List.of("numero", "inventada", "numero"),
            SustitutoVariables.listarNombres("{{numero}} y {{ inventada }} y {{numero}}"));
        assertEquals(List.of(), SustitutoVariables.listarNombres(null));
        assertEquals(List.of(), SustitutoVariables.listarNombres("sin variables { } {x}"));
    }

    @Test
    void testDeberiaEscaparLoQueHtmlInterpreta() {
        assertEquals("&lt;b&gt;A &amp; B&lt;/b&gt; &quot;c&quot; &#39;d&#39;", SustitutoVariables.escaparHtml("<b>A & B</b> \"c\" 'd'"));
        assertEquals("sin cambios", SustitutoVariables.escaparHtml("sin cambios"));
    }
}
