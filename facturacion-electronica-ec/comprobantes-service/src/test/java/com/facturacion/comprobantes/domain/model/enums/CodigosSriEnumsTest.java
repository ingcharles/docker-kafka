package com.facturacion.comprobantes.domain.model.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Los códigos del SRI se resuelven desde los enums del dominio, no desde constantes sueltas en cada clase. */
class CodigosSriEnumsTest {

    @Test
    void elImpuestoReconoceSuCodigoYTolerasNulo() {
        assertTrue(ImpuestoSri.IVA.esCodigo("2"));
        assertTrue(ImpuestoSri.ICE.esCodigo("3"));
        assertTrue(ImpuestoSri.IRBPNR.esCodigo("5"));
        assertFalse(ImpuestoSri.IVA.esCodigo("3"));
        assertFalse(ImpuestoSri.IVA.esCodigo(null));
    }

    @Test
    void losCodigosDeClasificacionDelIvaSonNoObjetoYExento() {
        assertTrue(CodigoIva.NO_OBJETO.esCodigo("6"));
        assertTrue(CodigoIva.EXENTO.esCodigo("7"));
        assertFalse(CodigoIva.EXENTO.esCodigo("4"));
    }

    @Test
    void elTipoDeComprobanteSeBuscaPorCodigoYTieneSuNombreImpreso() {
        assertEquals(TipoComprobante.NOTA_CREDITO, TipoComprobante.buscarPorCodigo("04").orElseThrow());
        assertEquals("LIQUIDACIÓN DE COMPRA", TipoComprobante.LIQUIDACION_COMPRA.nombre());
        assertTrue(TipoComprobante.buscarPorCodigo("99").isEmpty());
        for (TipoComprobante tipo : TipoComprobante.values()) {
            assertEquals(tipo, TipoComprobante.buscarPorCodigo(tipo.codigo()).orElseThrow());
        }
    }

    @Test
    void elTipoDeEmisionNormalEsElUno() {
        assertEquals("1", TipoEmision.NORMAL.codigo());
    }

    @Test
    void elFormatoDeTicketSeBuscaSinImportarMayusculas() {
        assertEquals(FormatoTicket.ESCPOS, FormatoTicket.buscarPorNombre("EscPos").orElseThrow());
        assertTrue(FormatoTicket.buscarPorNombre("pdf").isEmpty());
    }
}
