package com.facturacion.notificacion.domain.service;

/**
 * Texto con el que se guarda por qué falló el envío de un correo. Nunca es vacío: hay excepciones de red (p. ej.
 * {@code ClosedChannelException}) cuyo {@code getMessage()} es {@code null}, y un motivo ausente se confundiría con
 * "enviado sin error".
 */
public final class MotivoFalla {

    /** Tamaño de la columna {@code notificacion_enviada.motivo_error}. */
    static final int LONGITUD_MAXIMA = 500;

    private MotivoFalla() {
    }

    /** Describe la falla con su causa raíz: {@code "Clase: mensaje"} o solo {@code "Clase"} si no trae mensaje. */
    public static String obtenerMotivo(Throwable falla) {
        if (falla == null) {
            return "Falla desconocida al enviar el correo";
        }
        Throwable raiz = falla;
        while (raiz.getCause() != null && raiz.getCause() != raiz) {
            raiz = raiz.getCause();
        }
        String descripcion = describir(falla);
        if (raiz != falla) {
            descripcion += " (causa: " + describir(raiz) + ")";
        }
        return descripcion.length() <= LONGITUD_MAXIMA ? descripcion : descripcion.substring(0, LONGITUD_MAXIMA);
    }

    private static String describir(Throwable falla) {
        String mensaje = falla.getMessage();
        String clase = falla.getClass().getSimpleName();
        return mensaje == null || mensaje.isBlank() ? clase : clase + ": " + mensaje;
    }
}
