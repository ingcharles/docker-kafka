package com.facturacion.commons.correo;

import java.util.Arrays;
import java.util.List;

/**
 * Un campo de correo del receptor puede traer varios destinatarios separados por coma ({@code a@x.com,b@x.com}).
 * Comprobantes valida y normaliza la lista al crear el comprobante; notificación la parte de nuevo para armar el
 * campo {@code to} de un solo mensaje. Los dos servicios parten el texto con esta clase para no diferir en el separador.
 */
public final class ListaCorreos {

    /** Separador entre los correos de un mismo campo. */
    public static final String SEPARADOR = ",";

    private ListaCorreos() {
    }

    /**
     * Parte el texto por el separador y recorta cada pieza, sin descartar las vacías (así {@code "a@x.com,,b@x.com"} o
     * un separador al final se pueden rechazar). Texto nulo o en blanco = lista vacía.
     */
    public static List<String> partir(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        return Arrays.stream(texto.split(SEPARADOR, -1)).map(String::strip).toList();
    }

    /** Los correos del texto, recortados y sin piezas vacías. Texto nulo o en blanco = lista vacía. */
    public static List<String> separar(String texto) {
        return partir(texto).stream().filter(correo -> !correo.isEmpty()).toList();
    }

    /** Une los correos con el separador, en la forma en que se guardan. */
    public static String unir(List<String> correos) {
        return String.join(SEPARADOR, correos);
    }
}
