package com.facturacion.comprobantes.domain.model.enums;

import static com.facturacion.comprobantes.domain.model.enums.EstadoComprobante.ANULADO;
import static com.facturacion.comprobantes.domain.model.enums.EstadoComprobante.AUTORIZADO;
import static com.facturacion.comprobantes.domain.model.enums.EstadoComprobante.BORRADOR;
import static com.facturacion.comprobantes.domain.model.enums.EstadoComprobante.FIRMADO;
import static com.facturacion.comprobantes.domain.model.enums.EstadoComprobante.FIRMA_FALLIDA;
import static com.facturacion.comprobantes.domain.model.enums.EstadoComprobante.NOTIFICADO;
import static com.facturacion.comprobantes.domain.model.enums.EstadoComprobante.NO_AUTORIZADO;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EstadoComprobanteTest {

    @Test
    void testDeberiaAvanzarElCicloDeVidaPorElCaminoFeliz() {
        assertTrue(BORRADOR.puedePasarA(FIRMADO));
        assertTrue(FIRMADO.puedePasarA(AUTORIZADO));
        assertTrue(AUTORIZADO.puedePasarA(NOTIFICADO));
    }

    @Test
    void testDeberiaAvanzarTambienHaciaLosFinalesDeError() {
        assertTrue(BORRADOR.puedePasarA(FIRMA_FALLIDA));
        assertTrue(FIRMADO.puedePasarA(NO_AUTORIZADO));
    }

    @Test
    void testDeberiaNoRetrocederNiRepetirUnEstado() {
        assertFalse(AUTORIZADO.puedePasarA(FIRMADO));
        assertFalse(AUTORIZADO.puedePasarA(AUTORIZADO));
        assertFalse(NOTIFICADO.puedePasarA(AUTORIZADO));
        assertFalse(FIRMADO.puedePasarA(BORRADOR));
        assertFalse(FIRMADO.puedePasarA(FIRMADO));
    }

    @Test
    void testDeberiaDistinguirUnComandoQueSaltaPasosDeUnoRepetidoOAtrasado() {
        // Fuera de orden (el firmado no se aplicó): debe fallar visible, no descartarse.
        assertTrue(BORRADOR.esSaltoDePasos(AUTORIZADO));
        assertTrue(BORRADOR.esSaltoDePasos(NO_AUTORIZADO));
        assertTrue(BORRADOR.esSaltoDePasos(NOTIFICADO));
        assertTrue(FIRMADO.esSaltoDePasos(NOTIFICADO));
        // Repetidos, atrasados o ya válidos: no saltan pasos.
        assertFalse(FIRMADO.esSaltoDePasos(FIRMADO));
        assertFalse(AUTORIZADO.esSaltoDePasos(FIRMADO));
        assertFalse(AUTORIZADO.esSaltoDePasos(AUTORIZADO));
        assertFalse(BORRADOR.esSaltoDePasos(FIRMADO));
        assertFalse(FIRMADO.esSaltoDePasos(AUTORIZADO));
        assertFalse(NOTIFICADO.esSaltoDePasos(AUTORIZADO));
    }

    @Test
    void testDeberiaNoSaltarPasosNiSalirDeUnEstadoFinal() {
        assertFalse(BORRADOR.puedePasarA(AUTORIZADO));
        assertFalse(BORRADOR.puedePasarA(NOTIFICADO));
        for (EstadoComprobante destino : EstadoComprobante.values()) {
            assertFalse(FIRMA_FALLIDA.puedePasarA(destino), "FIRMA_FALLIDA -> " + destino);
            assertFalse(NO_AUTORIZADO.puedePasarA(destino), "NO_AUTORIZADO -> " + destino);
            assertFalse(NOTIFICADO.puedePasarA(destino), "NOTIFICADO -> " + destino);
            assertFalse(ANULADO.puedePasarA(destino), "ANULADO -> " + destino);
        }
    }
}
