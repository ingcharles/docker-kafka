package com.facturacion.comprobantes.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.facturacion.comprobantes.shared.error.CodigoError;
import com.facturacion.commons.exception.ErrorNegocioException;

/**
 * El valor retenido de una línea de retención debe ser el que resulta de su base y su porcentaje:
 * {@code baseImponible × porcentajeRetener / 100} redondeado a 2 decimales. Se tolera 1 centavo de diferencia
 * por el redondeo de quien calculó el valor. Sin esto, un cliente puede mandar un {@code valorRetenido}
 * cualquiera y el comprobante sale con un total que no corresponde a lo que dice retener. Lanza
 * {@link ErrorNegocioException} (400).
 */
public final class RetencionValidador {

    private static final BigDecimal TOLERANCIA = new BigDecimal("0.01");
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private RetencionValidador() {
    }

    public static void validarValorRetenido(BigDecimal baseImponible, BigDecimal porcentajeRetener,
            BigDecimal valorRetenido, String referencia) {
        BigDecimal esperado = baseImponible.multiply(porcentajeRetener).divide(CIEN, 2, RoundingMode.HALF_UP);
        if (valorRetenido.subtract(esperado).abs().compareTo(TOLERANCIA) > 0) {
            throw new ErrorNegocioException(CodigoError.VALOR_RETENIDO_INCORRECTO, referencia,
                valorRetenido.toPlainString(), baseImponible.toPlainString(), porcentajeRetener.toPlainString(),
                esperado.toPlainString());
        }
    }
}
