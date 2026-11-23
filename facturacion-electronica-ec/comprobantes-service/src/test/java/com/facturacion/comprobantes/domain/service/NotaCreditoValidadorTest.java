package com.facturacion.comprobantes.domain.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.facturacion.commons.exception.ApiException;
import com.facturacion.commons.time.ZonaHoraria;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.enums.EstadoComprobante;
import com.facturacion.comprobantes.shared.error.CodigoError;

class NotaCreditoValidadorTest {

    private static final String NUMERO = "001-002-000000123";
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 10);

    private static Comprobante documento(EstadoComprobante estado, String total) {
        Comprobante c = new Comprobante();
        c.setEstado(estado);
        c.setFechaEmision(FECHA.atTime(12, 0).toInstant(ZonaHoraria.ECUADOR));
        c.setImporteTotal(new BigDecimal(total));
        return c;
    }

    private static CodigoError codigoDe(Runnable accion) {
        return (CodigoError) assertThrows(ApiException.class, accion::run).getError();
    }

    @Test
    void testDeberiaPermitirModificarUnDocumentoAutorizadoHastaSuValor() {
        Comprobante factura = documento(EstadoComprobante.AUTORIZADO, "100.00");

        assertDoesNotThrow(() -> NotaCreditoValidador.validarContraDocumento(factura, NUMERO, FECHA,
            new BigDecimal("40.00"), new BigDecimal("60.00")));
        assertDoesNotThrow(() -> NotaCreditoValidador.validarContraDocumento(
            documento(EstadoComprobante.NOTIFICADO, "100.00"), NUMERO, FECHA, new BigDecimal("100.00"), BigDecimal.ZERO));
    }

    @Test
    void testDeberiaRechazarUnaNotaQueSuperaElValorDelDocumentoMenosLoYaAcreditado() {
        Comprobante factura = documento(EstadoComprobante.AUTORIZADO, "100.00");

        assertEquals(CodigoError.NC_EXCEDE_VALOR_DEL_DOCUMENTO, codigoDe(() -> NotaCreditoValidador
            .validarContraDocumento(factura, NUMERO, FECHA, new BigDecimal("40.01"), new BigDecimal("60.00"))));
        assertEquals(CodigoError.NC_EXCEDE_VALOR_DEL_DOCUMENTO, codigoDe(() -> NotaCreditoValidador
            .validarContraDocumento(factura, NUMERO, FECHA, new BigDecimal("100.01"), BigDecimal.ZERO)));
    }

    @Test
    void testDeberiaAplicarTambienLosControlesComunesDelDocumentoModificado() {
        assertEquals(CodigoError.DOCUMENTO_MODIFICADO_NO_AUTORIZADO, codigoDe(() -> NotaCreditoValidador
            .validarContraDocumento(documento(EstadoComprobante.FIRMADO, "100.00"), NUMERO, FECHA, BigDecimal.TEN,
                BigDecimal.ZERO)));
        assertEquals(CodigoError.DOCUMENTO_MODIFICADO_FECHA_NO_COINCIDE, codigoDe(() -> NotaCreditoValidador
            .validarContraDocumento(documento(EstadoComprobante.AUTORIZADO, "100.00"), NUMERO, FECHA.minusDays(1),
                BigDecimal.TEN, BigDecimal.ZERO)));
    }

    @Test
    void testDeberiaContarComoEmitidasSoloLasNotasEnCaminoOAutorizadas() {
        for (EstadoComprobante estado : EstadoComprobante.values()) {
            boolean cuenta = estado == EstadoComprobante.BORRADOR || estado == EstadoComprobante.FIRMADO
                || estado == EstadoComprobante.AUTORIZADO || estado == EstadoComprobante.NOTIFICADO;
            assertEquals(cuenta, estado.cuentaComoEmitido(), estado.name());
        }
        assertEquals(ZoneOffset.ofHours(-5), ZonaHoraria.ECUADOR);
    }
}
