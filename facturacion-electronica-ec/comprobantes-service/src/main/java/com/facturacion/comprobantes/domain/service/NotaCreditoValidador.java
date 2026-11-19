package com.facturacion.comprobantes.domain.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.facturacion.commons.exception.ErrorNegocioException;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.shared.error.CodigoError;

/**
 * Controles de una nota de crédito contra el documento que modifica: los comunes (ver
 * {@link DocumentoModificadoValidador}: formato del número, fecha, estado autorizado y fecha de emisión) más uno propio:
 * la nota, sumada a las notas de crédito ya emitidas sobre el documento, no supera su valor. Lanza
 * {@link ErrorNegocioException} (400).
 */
public final class NotaCreditoValidador {

    private NotaCreditoValidador() {
    }

    /**
     * Comprueba la nota contra el documento emitido por este sistema.
     *
     * @param original      el documento que se modifica
     * @param numero        su número, como lo escribió el cliente (para los mensajes)
     * @param fechaIndicada la fecha del documento que el cliente indicó en la nota
     * @param totalNota     el importe total de esta nota
     * @param yaEmitido     la suma de las notas de crédito vigentes ya emitidas sobre el mismo documento
     */
    public static void validarContraDocumento(Comprobante original, String numero, LocalDate fechaIndicada,
            BigDecimal totalNota, BigDecimal yaEmitido) {
        DocumentoModificadoValidador.validarContraDocumento(original, numero, fechaIndicada);
        if (yaEmitido.add(totalNota).compareTo(original.getImporteTotal()) > 0) {
            throw new ErrorNegocioException(CodigoError.NC_EXCEDE_VALOR_DEL_DOCUMENTO, totalNota, numero, yaEmitido,
                original.getImporteTotal());
        }
    }
}
