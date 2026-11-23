package com.facturacion.comprobantes.domain.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.facturacion.commons.exception.ErrorNegocioException;
import com.facturacion.comprobantes.shared.error.CodigoError;

class ConsumidorFinalMontoTest {

    private static final BigDecimal MAXIMO = new BigDecimal("50.00");

    private static void validar(String tipo, String total) {
        IdentificacionValidador.validarMontoConsumidorFinal(tipo, new BigDecimal(total), MAXIMO,
            "compradorTipoIdentificacion");
    }

    @Test
    void testDeberiaPermitirConsumidorFinalHastaCincuentaDolares() {
        assertDoesNotThrow(() -> validar("07", "50.00"));
        assertDoesNotThrow(() -> validar("07", "0.01"));
    }

    @Test
    void testDeberiaRechazarConsumidorFinalPorEncimaDeCincuentaDolares() {
        ErrorNegocioException e = assertThrows(ErrorNegocioException.class, () -> validar("07", "50.01"));
        assertEquals(CodigoError.CONSUMIDOR_FINAL_EXCEDE_MONTO, e.getCodigo());
    }

    @Test
    void testDeberiaNoAplicarTopeAOtrosTiposDeIdentificacion() {
        assertDoesNotThrow(() -> validar("05", "5000.00"));
        assertDoesNotThrow(() -> validar("04", "5000.00"));
    }

    @Test
    void testDeberiaTomarElMontoMaximoDeLaConfiguracion() {
        BigDecimal otro = new BigDecimal("200.00");
        assertDoesNotThrow(() -> IdentificacionValidador.validarMontoConsumidorFinal("07", new BigDecimal("150"), otro, "c"));
        assertThrows(ErrorNegocioException.class,
            () -> IdentificacionValidador.validarMontoConsumidorFinal("07", new BigDecimal("200.01"), otro, "c"));
    }

    @Test
    void testDeberiaNoHacerNadaSiFaltaElTipoOElMonto() {
        assertDoesNotThrow(() -> IdentificacionValidador.validarMontoConsumidorFinal(null, new BigDecimal("999"), MAXIMO, "c"));
        assertDoesNotThrow(() -> IdentificacionValidador.validarMontoConsumidorFinal("07", null, MAXIMO, "c"));
    }
}
