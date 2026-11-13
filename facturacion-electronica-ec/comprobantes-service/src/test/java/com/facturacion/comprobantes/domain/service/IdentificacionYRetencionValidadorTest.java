package com.facturacion.comprobantes.domain.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.facturacion.comprobantes.domain.model.enums.TipoIdentificacionSri;
import com.facturacion.comprobantes.shared.error.CodigoError;
import com.facturacion.commons.exception.ErrorNegocioException;

class IdentificacionYRetencionValidadorTest {

    private static final String RUC = TipoIdentificacionSri.RUC.codigo();
    private static final String CEDULA = TipoIdentificacionSri.CEDULA.codigo();
    private static final String PASAPORTE = TipoIdentificacionSri.PASAPORTE.codigo();
    private static final String CONSUMIDOR_FINAL = TipoIdentificacionSri.CONSUMIDOR_FINAL.codigo();
    private static final String EXTERIOR = TipoIdentificacionSri.IDENTIFICACION_EXTERIOR.codigo();
    private static final String TRECE_NUEVES = TipoIdentificacionSri.CONSUMIDOR_FINAL.numeroFijo().orElseThrow();

    /** Debe lanzar un ErrorNegocioException con ese código. */
    private static void rechaza(Executable accion, CodigoError esperado) {
        ErrorNegocioException error = assertThrows(ErrorNegocioException.class, accion);
        assertEquals(esperado, error.getCodigo());
    }

    @Test
    void consumidorFinalExigeTrece9() {
        assertTrue(TRECE_NUEVES.matches("9{13}"));
        assertDoesNotThrow(() -> IdentificacionValidador.validar(CONSUMIDOR_FINAL, TRECE_NUEVES, "c"));
        rechaza(() -> IdentificacionValidador.validar(CONSUMIDOR_FINAL, "1710034065", "c"),
            CodigoError.IDENTIFICACION_FIJA_INCORRECTA);
    }

    @Test
    void los9SoloSonValidosConElTipoConsumidorFinal() {
        for (String tipo : new String[] { RUC, CEDULA, PASAPORTE, EXTERIOR }) {
            rechaza(() -> IdentificacionValidador.validar(tipo, TRECE_NUEVES, "c"),
                CodigoError.IDENTIFICACION_FIJA_CON_OTRO_TIPO);
        }
    }

    @Test
    void rucTreceCedulaDiezDigitos() {
        assertDoesNotThrow(() -> IdentificacionValidador.validar(RUC, "1722039953001", "c"));
        assertDoesNotThrow(() -> IdentificacionValidador.validar(CEDULA, "1710034065", "c"));
        rechaza(() -> IdentificacionValidador.validar(RUC, "172203995300", "c"),
            CodigoError.IDENTIFICACION_LONGITUD_INVALIDA);
        rechaza(() -> IdentificacionValidador.validar(RUC, "17220399530AB", "c"),
            CodigoError.IDENTIFICACION_LONGITUD_INVALIDA);
        rechaza(() -> IdentificacionValidador.validar(CEDULA, "171003406", "c"),
            CodigoError.IDENTIFICACION_LONGITUD_INVALIDA);
    }

    @Test
    void pasaporteYExteriorSonDeFormatoLibre() {
        assertDoesNotThrow(() -> IdentificacionValidador.validar(PASAPORTE, "AB123456", "c"));
        assertDoesNotThrow(() -> IdentificacionValidador.validar(EXTERIOR, "X-99/2020", "c"));
    }

    @Test
    void unTipoQueNoEsDeLaTabla6LoDecideElCatalogoNoEsteValidador() {
        assertDoesNotThrow(() -> IdentificacionValidador.validar("99", "lo-que-sea", "c"));
    }

    @Test
    void valorRetenidoDebeSerBasePorPorcentaje() {
        assertDoesNotThrow(() -> RetencionValidador.validarValorRetenido(
            new BigDecimal("300.00"), new BigDecimal("10"), new BigDecimal("30.00"), "r"));
        // tolera 1 centavo de redondeo
        assertDoesNotThrow(() -> RetencionValidador.validarValorRetenido(
            new BigDecimal("33.33"), new BigDecimal("10"), new BigDecimal("3.34"), "r"));
        rechaza(() -> RetencionValidador.validarValorRetenido(
            new BigDecimal("300.00"), new BigDecimal("10"), new BigDecimal("45.00"), "r"),
            CodigoError.VALOR_RETENIDO_INCORRECTO);
        rechaza(() -> RetencionValidador.validarValorRetenido(
            new BigDecimal("300.00"), new BigDecimal("10"), new BigDecimal("30.05"), "r"),
            CodigoError.VALOR_RETENIDO_INCORRECTO);
    }
}
