package com.facturacion.comprobantes.domain.service;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.facturacion.commons.exception.ErrorNegocioException;
import com.facturacion.commons.time.FechasSri;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.enums.EstadoComprobante;
import com.facturacion.comprobantes.shared.error.CodigoError;

/**
 * Controles que una nota de crédito y una nota de débito comparten sobre el documento que modifican. Lanza
 * {@link ErrorNegocioException} (400).
 * <ul>
 *   <li>El número del documento tiene el formato {@code 001-001-000000001}.</li>
 *   <li>La fecha del documento no es futura.</li>
 *   <li>Si el documento es uno emitido por este sistema (para esa empresa): está autorizado por el SRI y la fecha
 *       indicada es la de su emisión.</li>
 * </ul>
 * Un documento que no está en este sistema (emitido en papel, por otro sistema o antes de este) no se puede
 * comprobar: de él solo se validan el formato y la fecha, y el SRI hace el resto al recibir la nota. Las reglas
 * propias de cada nota (p. ej. el valor de la de crédito) están en su validador.
 */
public final class DocumentoModificadoValidador {

    private static final Pattern NUMERO_DOCUMENTO = Pattern.compile("(\\d{3})-(\\d{3})-(\\d{9})");
    private static final int GRUPO_ESTABLECIMIENTO = 1;
    private static final int GRUPO_PUNTO_EMISION = 2;
    private static final int GRUPO_SECUENCIAL = 3;

    private DocumentoModificadoValidador() {
    }

    /** Las tres partes del número de un documento: {@code 001-002-000000123}. */
    public record NumeroDocumento(String establecimiento, String puntoEmision, String secuencial) {
    }

    /** Separa el número del documento que se modifica; falla si no tiene el formato del SRI. */
    public static NumeroDocumento interpretarNumero(String numero) {
        Matcher coincidencia = NUMERO_DOCUMENTO.matcher(numero == null ? "" : numero.trim());
        if (!coincidencia.matches()) {
            throw new ErrorNegocioException(CodigoError.DOCUMENTO_MODIFICADO_NUMERO_INVALIDO, numero);
        }
        return new NumeroDocumento(coincidencia.group(GRUPO_ESTABLECIMIENTO), coincidencia.group(GRUPO_PUNTO_EMISION),
            coincidencia.group(GRUPO_SECUENCIAL));
    }

    /** La fecha del documento que se modifica no puede ser posterior a hoy (hora de Ecuador). */
    public static void validarFecha(LocalDate fechaDocumento, LocalDate hoy) {
        if (fechaDocumento != null && fechaDocumento.isAfter(hoy)) {
            throw new ErrorNegocioException(CodigoError.DOCUMENTO_MODIFICADO_FECHA_FUTURA,
                FechasSri.formatearFecha(fechaDocumento), FechasSri.formatearFecha(hoy));
        }
    }

    /**
     * Comprueba la nota contra el documento emitido por este sistema: autorizado y con la fecha de emisión indicada.
     *
     * @param original      el documento que se modifica
     * @param numero        su número, como lo escribió el cliente (para los mensajes)
     * @param fechaIndicada la fecha del documento que el cliente indicó en la nota
     */
    public static void validarContraDocumento(Comprobante original, String numero, LocalDate fechaIndicada) {
        EstadoComprobante estado = original.getEstado();
        if (estado != EstadoComprobante.AUTORIZADO && estado != EstadoComprobante.NOTIFICADO) {
            throw new ErrorNegocioException(CodigoError.DOCUMENTO_MODIFICADO_NO_AUTORIZADO, numero, estado);
        }
        LocalDate fechaDocumento = FechasSri.convertirAFecha(original.getFechaEmision());
        if (fechaIndicada != null && !fechaIndicada.equals(fechaDocumento)) {
            throw new ErrorNegocioException(CodigoError.DOCUMENTO_MODIFICADO_FECHA_NO_COINCIDE,
                FechasSri.formatearFecha(fechaIndicada), numero, FechasSri.formatearFecha(fechaDocumento));
        }
    }
}
