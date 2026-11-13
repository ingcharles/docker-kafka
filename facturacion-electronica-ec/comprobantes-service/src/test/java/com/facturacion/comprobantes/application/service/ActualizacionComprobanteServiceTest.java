package com.facturacion.comprobantes.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.facturacion.comprobantes.domain.model.enums.EstadoComprobante;
import com.facturacion.comprobantes.shared.constant.MessagingConstants;

class ActualizacionComprobanteServiceTest {

    @Test
    void cadaComandoLlevaAlEstadoQueLeCorresponde() {
        assertEquals(EstadoComprobante.FIRMADO,
            ActualizacionComprobanteService.estadoDestino(MessagingConstants.COMANDO_MARCAR_FIRMADO));
        assertEquals(EstadoComprobante.FIRMA_FALLIDA,
            ActualizacionComprobanteService.estadoDestino(MessagingConstants.COMANDO_MARCAR_FIRMA_FALLIDA));
        assertEquals(EstadoComprobante.NO_AUTORIZADO,
            ActualizacionComprobanteService.estadoDestino(MessagingConstants.COMANDO_MARCAR_NO_AUTORIZADO));
        assertEquals(EstadoComprobante.AUTORIZADO,
            ActualizacionComprobanteService.estadoDestino(MessagingConstants.COMANDO_MARCAR_AUTORIZADO));
        assertEquals(EstadoComprobante.NOTIFICADO,
            ActualizacionComprobanteService.estadoDestino(MessagingConstants.COMANDO_MARCAR_NOTIFICADO));
    }

    @Test
    void unComandoDesconocidoEsUnError() {
        assertThrows(IllegalArgumentException.class, () -> ActualizacionComprobanteService.estadoDestino("OTRO"));
    }
}
