package com.facturacion.notificacion.domain.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Saca la versión en texto plano de un HTML sencillo (el que se guarda ya limpiado): los cortes de párrafo, fila y salto
 * pasan a líneas, las celdas a espacios y las etiquetas se quitan. Sirve cuando una plantilla no trae su propio texto.
 */
public final class HtmlATexto {

    private static final Pattern SALTO = Pattern.compile("(?i)<br\\s*/?>");
    private static final Pattern FIN_DE_BLOQUE = Pattern.compile("(?i)</(p|div|tr|table|ul|ol|li|h[1-6]|blockquote)\\s*>");
    private static final Pattern FIN_DE_CELDA = Pattern.compile("(?i)</t[dh]\\s*>");
    private static final Pattern ETIQUETA = Pattern.compile("<[^>]*>");
    private static final Pattern ENTIDAD_DECIMAL = Pattern.compile("&#(\\d+);");
    private static final Pattern ENTIDAD_HEXADECIMAL = Pattern.compile("&#[xX]([0-9a-fA-F]+);");
    private static final Pattern ESPACIOS = Pattern.compile("[ \\t\\u00A0]+");
    private static final Pattern ESPACIOS_ALREDEDOR_DE_SALTOS = Pattern.compile(" *\\n *");
    private static final Pattern SALTOS_SOBRANTES = Pattern.compile("\\n{3,}");
    private static final String NUEVA_LINEA = "\n";
    private static final String ESPACIO = " ";
    private static final String VACIO = "";
    private static final int BASE_HEXADECIMAL = 16;

    private HtmlATexto() {
    }

    public static String convertir(String html) {
        String texto = SALTO.matcher(html).replaceAll(NUEVA_LINEA);
        texto = FIN_DE_BLOQUE.matcher(texto).replaceAll(NUEVA_LINEA);
        texto = FIN_DE_CELDA.matcher(texto).replaceAll(ESPACIO);
        texto = ETIQUETA.matcher(texto).replaceAll(VACIO);
        texto = reemplazarEntidades(texto);
        texto = ESPACIOS.matcher(texto).replaceAll(ESPACIO);
        texto = ESPACIOS_ALREDEDOR_DE_SALTOS.matcher(texto).replaceAll(NUEVA_LINEA);
        texto = SALTOS_SOBRANTES.matcher(texto).replaceAll(NUEVA_LINEA + NUEVA_LINEA);
        return texto.trim();
    }

    private static String reemplazarEntidades(String texto) {
        String resultado = reemplazarNumericas(texto, ENTIDAD_DECIMAL, 10);
        resultado = reemplazarNumericas(resultado, ENTIDAD_HEXADECIMAL, BASE_HEXADECIMAL);
        // &amp; va al final: si no, "&amp;lt;" se leería como "<" en vez de "&lt;".
        return resultado.replace("&nbsp;", ESPACIO).replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
            .replace("&apos;", "'").replace("&amp;", "&");
    }

    private static String reemplazarNumericas(String texto, Pattern patron, int base) {
        Matcher coincidencia = patron.matcher(texto);
        StringBuilder resultado = new StringBuilder();
        while (coincidencia.find()) {
            String caracter = convertirACaracter(coincidencia.group(1), base);
            coincidencia.appendReplacement(resultado, Matcher.quoteReplacement(caracter));
        }
        coincidencia.appendTail(resultado);
        return resultado.toString();
    }

    private static String convertirACaracter(String numero, int base) {
        try {
            return new String(Character.toChars(Integer.parseInt(numero, base)));
        } catch (IllegalArgumentException e) {
            return VACIO;
        }
    }
}
