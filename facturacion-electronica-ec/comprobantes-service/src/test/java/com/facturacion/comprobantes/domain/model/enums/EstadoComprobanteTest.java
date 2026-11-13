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
    void elCicloDeVidaAvanzaPorElCaminoFeliz() {
        assertTrue(BORRADOR.puedePasarA(FIRMADO));
        assertTrue(FIRMADO.puedePasarA(AUTORIZADO));
        assertTrue(AUTORIZADO.puedePasarA(NOTIFICADO));
    }

    @Test
    void tambienAvanzaHaciaLosFinalesDeError() {
        assertTrue(BORRADOR.puedePasarA(FIRMA_FALLIDA));
        assertTrue(FIRMADO.puedePasarA(NO_AUTORIZADO));
    }

    @Test
    void nuncaRetrocedeNiSeRepite() {
        assertFalse(AUTORIZADO.puedePasarA(FIRMADO));
        assertFalse(AUTORIZADO.puedePasarA(AUTORIZADO));
        assertFalse(NOTIFICADO.puedePasarA(AUTORIZADO));
        assertFalse(FIRMADO.puedePasarA(BORRADOR));
        assertFalse(FIRMADO.puedePasarA(FIRMADO));
    }

    @Test
    void unComandoQueSaltaPasosSeDistingueDeUnoRepetidoOAtrasado() {
        // Fuera de orden (el firmado no se aplicó): debe fallar visible, no descartarse.
        assertTrue(BORRADOR.saltaPasos(AUTORIZADO));
        assertTrue(BORRADOR.saltaPasos(NO_AUTORIZADO));
        assertTrue(BORRADOR.saltaPasos(NOTIFICADO));
        assertTrue(FIRMADO.saltaPasos(NOTIFICADO));
        // Repetidos, atrasados o ya válidos: no saltan pasos.
        assertFalse(FIRMADO.saltaPasos(FIRMADO));
        assertFalse(AUTORIZADO.saltaPasos(FIRMADO));
        assertFalse(AUTORIZADO.saltaPasos(AUTORIZADO));
        assertFalse(BORRADOR.saltaPasos(FIRMADO));
        assertFalse(FIRMADO.saltaPasos(AUTORIZADO));
        assertFalse(NOTIFICADO.saltaPasos(AUTORIZADO));
    }

    @Test
    void noSeSaltaPasosNiSalePorUnEstadoFinal() {
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
