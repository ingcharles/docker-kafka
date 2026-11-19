package com.facturacion.comprobantes.domain.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.facturacion.commons.exception.ApiException;
import com.facturacion.commons.time.ZonaHoraria;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.enums.EstadoComprobante;
import com.facturacion.comprobantes.domain.service.DocumentoModificadoValidador.NumeroDocumento;
import com.facturacion.comprobantes.shared.error.CodigoError;

class DocumentoModificadoValidadorTest {

    private static final String NUMERO = "001-002-000000123";
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 10);

    private static Comprobante documento(EstadoComprobante estado) {
        Comprobante c = new Comprobante();
        c.setEstado(estado);
        c.setFechaEmision(FECHA.atTime(12, 0).toInstant(ZonaHoraria.ECUADOR));
        c.setImporteTotal(new BigDecimal("100.00"));
        return c;
    }

    private static CodigoError codigoDe(Runnable accion) {
        return (CodigoError) assertThrows(ApiException.class, accion::run).getError();
    }

    @Test
    void elNumeroSeSeparaEnEstablecimientoPuntoYSecuencial() {
        NumeroDocumento numero = DocumentoModificadoValidador.interpretarNumero(NUMERO);

        assertEquals("001", numero.establecimiento());
        assertEquals("002", numero.puntoEmision());
        assertEquals("000000123", numero.secuencial());
    }

    @Test
    void unNumeroConOtroFormatoSeRechaza() {
        for (String invalido : new String[] {null, "", "1-1-1", "001-002-123", "001002000000123", "001-002-0000001234", "aaa-bbb-cccccccccc"}) {
            assertEquals(CodigoError.DOCUMENTO_MODIFICADO_NUMERO_INVALIDO,
                codigoDe(() -> DocumentoModificadoValidador.interpretarNumero(invalido)));
        }
    }

    @Test
    void laFechaDelDocumentoNoPuedeSerFutura() {
        LocalDate hoy = LocalDate.of(2026, 11, 19);

        assertDoesNotThrow(() -> DocumentoModificadoValidador.validarFecha(hoy, hoy));
        assertDoesNotThrow(() -> DocumentoModificadoValidador.validarFecha(hoy.minusDays(400), hoy));
        assertEquals(CodigoError.DOCUMENTO_MODIFICADO_FECHA_FUTURA,
            codigoDe(() -> DocumentoModificadoValidador.validarFecha(hoy.plusDays(1), hoy)));
    }

    @Test
    void unDocumentoAutorizadoONotificadoSePuedeModificar() {
        assertDoesNotThrow(() -> DocumentoModificadoValidador.validarContraDocumento(
            documento(EstadoComprobante.AUTORIZADO), NUMERO, FECHA));
        assertDoesNotThrow(() -> DocumentoModificadoValidador.validarContraDocumento(
            documento(EstadoComprobante.NOTIFICADO), NUMERO, FECHA));
    }

    @Test
    void soloSeModificaUnDocumentoAutorizado() {
        for (EstadoComprobante estado : EstadoComprobante.values()) {
            if (estado == EstadoComprobante.AUTORIZADO || estado == EstadoComprobante.NOTIFICADO) {
                continue;
            }
            Comprobante factura = documento(estado);
            assertEquals(CodigoError.DOCUMENTO_MODIFICADO_NO_AUTORIZADO, codigoDe(() -> DocumentoModificadoValidador
                .validarContraDocumento(factura, NUMERO, FECHA)), estado.name());
        }
    }

    @Test
    void laFechaIndicadaDebeSerLaDeEmisionDelDocumento() {
        Comprobante factura = documento(EstadoComprobante.AUTORIZADO);

        assertEquals(CodigoError.DOCUMENTO_MODIFICADO_FECHA_NO_COINCIDE, codigoDe(() -> DocumentoModificadoValidador
            .validarContraDocumento(factura, NUMERO, FECHA.minusDays(1))));
    }
}
