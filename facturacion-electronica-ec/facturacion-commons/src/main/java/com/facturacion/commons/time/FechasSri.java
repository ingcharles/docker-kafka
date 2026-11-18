package com.facturacion.commons.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * Formato y lectura de las fechas del SRI y de los documentos impresos, siempre en hora de Ecuador
 * ({@link ZonaHoraria#ECUADOR}). Es el único lugar con los patrones de fecha: ningún servicio arma su propio
 * {@code DateTimeFormatter}. Los instantes se guardan en UTC; aquí se pasan a la fecha local que ve el contribuyente.
 */
public final class FechasSri {

    /** dd/MM/aaaa: la fecha de los XML del SRI, del RIDE y de los textos de error. */
    public static final String PATRON_FECHA = "dd/MM/yyyy";
    /** dd/MM/aaaa HH:mm:ss: fecha y hora de autorización en el RIDE. */
    public static final String PATRON_FECHA_HORA = "dd/MM/yyyy HH:mm:ss";
    /** dd/MM/aaaa HH:mm: fecha y hora cuando el espacio es corto (ticket). */
    public static final String PATRON_FECHA_HORA_CORTA = "dd/MM/yyyy HH:mm";
    /** ddMMaaaa: la fecha dentro de la clave de acceso. */
    public static final String PATRON_CLAVE_ACCESO = "ddMMyyyy";
    /** dd/MM/aaaa para leer: con año de era ({@code uuuu}) para que el modo estricto rechace el 31/02. */
    private static final String PATRON_FECHA_ESTRICTA = "dd/MM/uuuu";

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern(PATRON_FECHA)
        .withZone(ZonaHoraria.ECUADOR);
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern(PATRON_FECHA_HORA)
        .withZone(ZonaHoraria.ECUADOR);
    private static final DateTimeFormatter FECHA_HORA_CORTA = DateTimeFormatter.ofPattern(PATRON_FECHA_HORA_CORTA)
        .withZone(ZonaHoraria.ECUADOR);
    private static final DateTimeFormatter CLAVE_ACCESO = DateTimeFormatter.ofPattern(PATRON_CLAVE_ACCESO);
    private static final DateTimeFormatter FECHA_ESTRICTA = DateTimeFormatter.ofPattern(PATRON_FECHA_ESTRICTA)
        .withResolverStyle(ResolverStyle.STRICT);

    private FechasSri() {
    }

    /** dd/MM/aaaa de un instante, en hora de Ecuador. */
    public static String formatearFecha(Instant instante) {
        return FECHA.format(instante);
    }

    public static String formatearFecha(LocalDate fecha) {
        return FECHA.format(fecha);
    }

    /** dd/MM/aaaa HH:mm:ss de un instante, en hora de Ecuador. */
    public static String formatearFechaHora(Instant instante) {
        return FECHA_HORA.format(instante);
    }

    /** dd/MM/aaaa HH:mm de un instante, en hora de Ecuador. */
    public static String formatearFechaHoraCorta(Instant instante) {
        return FECHA_HORA_CORTA.format(instante);
    }

    /** ddMMaaaa para la clave de acceso. */
    public static String formatearFechaClaveAcceso(LocalDate fecha) {
        return CLAVE_ACCESO.format(fecha);
    }

    /**
     * Lee una fecha dd/MM/aaaa de forma estricta: una fecha que no existe (31/02/2026) o un texto que no es fecha lanzan
     * {@link DateTimeParseException} en vez de corregirse en silencio.
     */
    public static LocalDate leerFecha(String ddMmAaaa) {
        return LocalDate.parse(ddMmAaaa, FECHA_ESTRICTA);
    }

    /** El inicio del día (00:00 hora de Ecuador) de una fecha dd/MM/aaaa; mismas reglas que {@link #leerFecha}. */
    public static Instant obtenerInicioDelDia(String ddMmAaaa) {
        return leerFecha(ddMmAaaa).atStartOfDay(ZonaHoraria.ECUADOR).toInstant();
    }

    /** La fecha calendario, en Ecuador, en la que cae un instante. */
    public static LocalDate convertirAFecha(Instant instante) {
        return instante.atOffset(ZonaHoraria.ECUADOR).toLocalDate();
    }

    /** La fecha de hoy en Ecuador (no la del servidor, que puede estar en UTC y ya ser "mañana" a las 19:00). */
    public static LocalDate obtenerHoy() {
        return LocalDate.now(ZonaHoraria.ECUADOR);
    }
}
