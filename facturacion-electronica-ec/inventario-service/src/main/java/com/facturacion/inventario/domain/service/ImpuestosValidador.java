package com.facturacion.inventario.domain.service;

import java.math.BigDecimal;

import com.facturacion.commons.exception.ErrorNegocioException;
import com.facturacion.inventario.shared.error.CodigoError;

/**
 * Coherencia de la clasificación tributaria de un ítem. Lanza {@link ErrorNegocioException} con su
 * {@link CodigoError} (400). Este servicio solo guarda los códigos (codigoImpuesto + codigoPorcentaje); que existan y
 * estén vigentes lo valida comprobantes-service al emitir, que es el dueño de los catálogos del SRI. Lo único que se
 * comprueba aquí es lo que no depende de ningún catálogo: que un ítem con ICE traiga su código y su tarifa.
 */
public final class ImpuestosValidador {

    private ImpuestosValidador() {
    }

    /**
     * Un ítem con ICE necesita su código de porcentaje y su tarifa: sin ellos comprobantes-service no puede
     * calcular el impuesto al facturarlo (fallaba recién al emitir, con un error interno).
     */
    public static void validarIce(boolean tieneIce, String codigoPorcentajeIce, BigDecimal tarifaIce) {
        if (!tieneIce) {
            return;
        }
        if (codigoPorcentajeIce == null || codigoPorcentajeIce.isBlank()) {
            throw new ErrorNegocioException(CodigoError.ICE_SIN_CODIGO_PORCENTAJE);
        }
        if (tarifaIce == null || tarifaIce.signum() < 0) {
            throw new ErrorNegocioException(CodigoError.ICE_SIN_TARIFA);
        }
    }

}
