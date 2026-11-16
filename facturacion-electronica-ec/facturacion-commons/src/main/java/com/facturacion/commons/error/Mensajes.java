package com.facturacion.commons.error;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Texto de cada entrada del catálogo. Busca la clave primero en el {@code mensajes.properties} del servicio (si lo
 * tiene) y luego en {@code mensajes-comunes.properties} de este jar. Los parámetros reemplazan {@code {0}},
 * {@code {1}}... tal cual (sin formato de números ni reglas de comillas, a diferencia de {@code MessageFormat}):
 * quien llama los pasa ya como texto.
 */
public final class Mensajes {

    private static final String BUNDLE_SERVICIO = "mensajes";
    private static final String BUNDLE_COMUN = "mensajes-comunes";

    private static final ResourceBundle SERVICIO = cargar(BUNDLE_SERVICIO);
    private static final ResourceBundle COMUN = cargar(BUNDLE_COMUN);

    private Mensajes() {
    }

    public static String obtenerTexto(MensajeCatalogo entrada, Object... parametros) {
        String texto = plantilla(entrada.claveMensaje());
        for (int i = 0; i < parametros.length; i++) {
            texto = texto.replace("{" + i + "}", String.valueOf(parametros[i]));
        }
        return texto;
    }

    /** Si la clave está en el {@code mensajes.properties} del servicio. */
    public static boolean existe(String clave) {
        return SERVICIO != null && SERVICIO.containsKey(clave);
    }

    /** Todas las claves del {@code mensajes.properties} del servicio (vacío si no tiene). */
    public static Set<String> listarClaves() {
        return SERVICIO == null ? Set.of() : SERVICIO.keySet();
    }

    /** Si la clave está en los mensajes comunes de este jar. */
    public static boolean existeComun(String clave) {
        return COMUN != null && COMUN.containsKey(clave);
    }

    private static String plantilla(String clave) {
        if (SERVICIO != null && SERVICIO.containsKey(clave)) {
            return SERVICIO.getString(clave);
        }
        if (COMUN != null && COMUN.containsKey(clave)) {
            return COMUN.getString(clave);
        }
        throw new MissingResourceException("No hay texto para la clave de mensaje '" + clave + "'",
            Mensajes.class.getName(), clave);
    }

    private static ResourceBundle cargar(String nombre) {
        ClassLoader contexto = Thread.currentThread().getContextClassLoader();
        try {
            return ResourceBundle.getBundle(nombre, Locale.ROOT, contexto != null ? contexto : Mensajes.class.getClassLoader());
        } catch (MissingResourceException e) {
            return null;
        }
    }
}
