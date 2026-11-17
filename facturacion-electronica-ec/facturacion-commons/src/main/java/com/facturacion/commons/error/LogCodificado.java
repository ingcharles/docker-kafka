package com.facturacion.commons.error;

import org.jboss.logging.Logger;

/**
 * Log con su código de catálogo: {@code [FIR-401] texto (contexto)}. El código lleva el prefijo del servicio y un número
 * cuyo primer dígito es el nivel (1 DEBUG, 2 INFO, 3 WARN, 4 ERROR, 5 FATAL), así que el nivel se lee en el propio
 * código; {@code scripts/verificar-logs.sh} comprueba que el método usado coincida con ese dígito. Un código estable
 * permite filtrar y alertar sin depender de la redacción, y tener un runbook por código. El texto sale del
 * {@code mensajes.properties} del servicio, igual que el de los errores. {@code contexto} lleva solo identificadores
 * ({@code comprobanteId=…}, {@code claveAcceso=…}); nunca datos personales, tokens ni contraseñas.
 */
public final class LogCodificado {

    private LogCodificado() {
    }

    public static void debug(Logger log, MensajeCatalogo evento, String contexto, Object... parametros) {
        if (log.isDebugEnabled()) {
            log.debug(formatear(evento, contexto, parametros));
        }
    }

    public static void info(Logger log, MensajeCatalogo evento, String contexto, Object... parametros) {
        if (log.isInfoEnabled()) {
            log.info(formatear(evento, contexto, parametros));
        }
    }

    public static void warn(Logger log, MensajeCatalogo evento, String contexto, Object... parametros) {
        log.warn(formatear(evento, contexto, parametros));
    }

    public static void error(Logger log, MensajeCatalogo evento, String contexto, Object... parametros) {
        log.error(formatear(evento, contexto, parametros));
    }

    public static void error(Logger log, Throwable causa, MensajeCatalogo evento, String contexto, Object... parametros) {
        log.error(formatear(evento, contexto, parametros), causa);
    }

    public static void fatal(Logger log, MensajeCatalogo evento, String contexto, Object... parametros) {
        log.fatal(formatear(evento, contexto, parametros));
    }

    public static void fatal(Logger log, Throwable causa, MensajeCatalogo evento, String contexto, Object... parametros) {
        log.fatal(formatear(evento, contexto, parametros), causa);
    }

    static String formatear(MensajeCatalogo evento, String contexto, Object... parametros) {
        String texto = "[" + evento.codigo() + "] " + Mensajes.obtenerTexto(evento, parametros);
        return contexto == null || contexto.isBlank() ? texto : texto + " (" + contexto + ")";
    }
}
