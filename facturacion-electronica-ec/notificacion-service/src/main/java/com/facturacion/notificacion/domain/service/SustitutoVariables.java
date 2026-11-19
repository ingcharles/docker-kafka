package com.facturacion.notificacion.domain.service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.facturacion.notificacion.domain.model.enums.VariableCorreo;

/**
 * Reemplaza las variables {@code {{nombre}}} de una plantilla de empresa por sus valores. Es un reemplazo de texto, no un
 * lenguaje de plantillas: no evalúa expresiones ni llama a nada, así que quien edita la plantilla no puede ejecutar código.
 */
public final class SustitutoVariables {

    /** Cualquier {@code {{...}}} (con el nombre que sea): sirve para encontrar también las variables que no existen. */
    private static final Pattern MARCADOR = Pattern.compile("\\{\\{([^{}]*)}}");
    private static final String VACIO = "";

    private SustitutoVariables() {
    }

    /** Los nombres de las variables que aparecen en el texto (sin llaves ni espacios), en orden y con repeticiones. */
    public static List<String> listarNombres(String plantilla) {
        List<String> nombres = new ArrayList<>();
        Matcher coincidencia = MARCADOR.matcher(plantilla == null ? VACIO : plantilla);
        while (coincidencia.find()) {
            nombres.add(coincidencia.group(1).trim());
        }
        return nombres;
    }

    /** Reemplaza cada variable por lo que devuelva {@code valores}; una variable que no existe se reemplaza por vacío. */
    public static String sustituir(String plantilla, Function<VariableCorreo, String> valores) {
        Matcher coincidencia = MARCADOR.matcher(plantilla);
        StringBuilder resultado = new StringBuilder();
        while (coincidencia.find()) {
            String valor = VariableCorreo.obtenerPorNombre(coincidencia.group(1).trim()).map(valores).orElse(VACIO);
            coincidencia.appendReplacement(resultado, Matcher.quoteReplacement(valor == null ? VACIO : valor));
        }
        coincidencia.appendTail(resultado);
        return resultado.toString();
    }

    /** Escapa lo que HTML interpreta ({@code & < > " '}): el valor se ve tal cual y nunca se lee como etiqueta. */
    public static String escaparHtml(String texto) {
        StringBuilder escapado = new StringBuilder(texto.length() + 16);
        for (char caracter : texto.toCharArray()) {
            switch (caracter) {
                case '&' -> escapado.append("&amp;");
                case '<' -> escapado.append("&lt;");
                case '>' -> escapado.append("&gt;");
                case '"' -> escapado.append("&quot;");
                case '\'' -> escapado.append("&#39;");
                default -> escapado.append(caracter);
            }
        }
        return escapado.toString();
    }
}
