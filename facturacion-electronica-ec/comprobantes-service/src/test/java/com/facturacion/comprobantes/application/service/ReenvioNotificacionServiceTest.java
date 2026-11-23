package com.facturacion.comprobantes.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.facturacion.commons.exception.ConflictoException;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.enums.EstadoComprobante;
import com.facturacion.comprobantes.shared.error.CodigoError;

class ReenvioNotificacionServiceTest {

    private static Comprobante comprobante(EstadoComprobante estado, byte[] ride, String xml, String correo) {
        Comprobante c = new Comprobante();
        c.setEstado(estado);
        c.setRidePdf(ride);
        c.setXmlAutorizado(xml);
        c.setCompradorCorreo(correo);
        return c;
    }

    @Test
    void testDeberiaReenviarUnComprobanteAutorizadoOYaNotificado() {
        byte[] ride = {1};
        assertDoesNotThrow(() -> ReenvioNotificacionService.exigirReenviable(
            comprobante(EstadoComprobante.AUTORIZADO, ride, "<autorizacion/>", "a@b.com")));
        assertDoesNotThrow(() -> ReenvioNotificacionService.exigirReenviable(
            comprobante(EstadoComprobante.NOTIFICADO, ride, "<autorizacion/>", "a@b.com")));
    }

    @Test
    void testDeberiaRechazarElReenvioDeUnComprobanteSinAutorizar() {
        ConflictoException e = assertThrows(ConflictoException.class, () -> ReenvioNotificacionService
            .exigirReenviable(comprobante(EstadoComprobante.FIRMADO, null, null, "a@b.com")));

        assertEquals(CodigoError.REENVIO_NO_AUTORIZADO, e.getError());
    }

    @Test
    void testDeberiaRechazarElReenvioSiElComprobanteNoTieneRide() {
        ConflictoException e = assertThrows(ConflictoException.class, () -> ReenvioNotificacionService
            .exigirReenviable(comprobante(EstadoComprobante.AUTORIZADO, null, "<autorizacion/>", "a@b.com")));

        assertEquals(CodigoError.REENVIO_NO_AUTORIZADO, e.getError());
    }

    @Test
    void testDeberiaRechazarElReenvioSiElReceptorNoTieneCorreo() {
        byte[] ride = {1};
        for (String correo : new String[] {null, "  "}) {
            ConflictoException e = assertThrows(ConflictoException.class, () -> ReenvioNotificacionService
                .exigirReenviable(comprobante(EstadoComprobante.AUTORIZADO, ride, "<autorizacion/>", correo)));

            assertEquals(CodigoError.REENVIO_SIN_CORREO, e.getError());
        }
    }
}
