package com.facturacion.inventario.domain.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.facturacion.inventario.shared.error.CodigoError;
import com.facturacion.commons.exception.ErrorNegocioException;

class ImpuestosValidadorTest {

    /** Debe lanzar un ErrorNegocioException con ese código. */
    private static void rechaza(Executable accion, CodigoError esperado) {
        ErrorNegocioException error = assertThrows(ErrorNegocioException.class, accion);
        assertEquals(esperado, error.getCodigo());
    }

    @Test
    void itemSinIceNoExigeNada() {
        assertDoesNotThrow(() -> ImpuestosValidador.validarIce(false, null, null));
    }

    @Test
    void itemConIceCompletoEsValido() {
        assertDoesNotThrow(() -> ImpuestosValidador.validarIce(true, "3011", new BigDecimal("75")));
        assertDoesNotThrow(() -> ImpuestosValidador.validarIce(true, "3011", BigDecimal.ZERO));
    }

    @Test
    void itemConIceExigeCodigoDePorcentaje() {
        rechaza(() -> ImpuestosValidador.validarIce(true, null, new BigDecimal("75")),
            CodigoError.ICE_SIN_CODIGO_PORCENTAJE);
        rechaza(() -> ImpuestosValidador.validarIce(true, " ", new BigDecimal("75")),
            CodigoError.ICE_SIN_CODIGO_PORCENTAJE);
    }

    @Test
    void itemConIceExigeTarifaNoNegativa() {
        rechaza(() -> ImpuestosValidador.validarIce(true, "3011", null), CodigoError.ICE_SIN_TARIFA);
        rechaza(() -> ImpuestosValidador.validarIce(true, "3011", new BigDecimal("-1")), CodigoError.ICE_SIN_TARIFA);
    }
}
