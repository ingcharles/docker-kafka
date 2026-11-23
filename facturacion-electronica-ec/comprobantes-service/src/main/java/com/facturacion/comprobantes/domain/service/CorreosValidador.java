package com.facturacion.comprobantes.domain.service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import com.facturacion.commons.correo.ListaCorreos;
import com.facturacion.commons.exception.ErrorNegocioException;
import com.facturacion.comprobantes.shared.error.CodigoError;

/**
 * El correo del receptor puede ser una lista separada por coma (ver {@link ListaCorreos}). Se valida al crear el
 * comprobante para no aceptar un 201 y fallar después en el envío: cada correo tiene forma válida, no hay repetidos y
 * la lista no pasa del máximo ni de lo que cabe en la columna. Lanza {@link ErrorNegocioException} (400).
 */
public final class CorreosValidador {

    /** Máximo de destinatarios por comprobante si no se configura {@code comprobantes.correo.maximo-destinatarios}. */
    public static final String MAXIMO_POR_DEFECTO = "3";

    /** Largo de la columna {@code comprobante.comprador_correo}: la lista ya normalizada debe caber. */
    public static final int LONGITUD_MAXIMA = 300;

    private static final Pattern FORMA_DE_CORREO = Pattern.compile("[^\\s@,;<>]+@[^\\s@,;<>]+\\.[^\\s@,;<>]+");

    private CorreosValidador() {
    }

    /**
     * Devuelve la lista normalizada (cada correo recortado, unidos por coma) o {@code null} si el campo viene vacío
     * (el correo es opcional).
     */
    public static String validarYNormalizar(String texto, String campo, int maximo) {
        List<String> correos = ListaCorreos.partir(texto);
        if (correos.isEmpty()) {
            return null;
        }
        for (String correo : correos) {
            if (!FORMA_DE_CORREO.matcher(correo).matches()) {
                throw new ErrorNegocioException(CodigoError.CORREO_INVALIDO, campo, correo);
            }
        }
        if (correos.size() > maximo) {
            throw new ErrorNegocioException(CodigoError.CORREOS_EXCEDEN_MAXIMO, campo, maximo, correos.size());
        }
        if (correos.stream().map(c -> c.toLowerCase(Locale.ROOT)).distinct().count() != correos.size()) {
            throw new ErrorNegocioException(CodigoError.CORREOS_REPETIDOS, campo);
        }
        String normalizado = ListaCorreos.unir(correos);
        if (normalizado.length() > LONGITUD_MAXIMA) {
            throw new ErrorNegocioException(CodigoError.CORREOS_MUY_LARGOS, campo, LONGITUD_MAXIMA);
        }
        return normalizado;
    }
}
