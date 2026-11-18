package com.facturacion.comprobantes.domain.model.entity;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.facturacion.comprobantes.domain.model.enums.EstadoComprobante;

/** El ciclo de vida vive en el agregado: cada transición deja el estado y sus datos juntos, y rechaza las ilegales. */
class ComprobanteCicloDeVidaTest {

    private static Comprobante borrador() {
        Comprobante comprobante = new Comprobante();
        comprobante.setId(UUID.randomUUID());
        comprobante.setEstado(EstadoComprobante.BORRADOR);
        return comprobante;
    }

    @Test
    void recorreElCicloFelizGuardandoLosDatosDeCadaPaso() {
        Comprobante comprobante = borrador();

        comprobante.marcarFirmado("<firmado/>");
        assertEquals(EstadoComprobante.FIRMADO, comprobante.getEstado());
        assertEquals("<firmado/>", comprobante.getXmlFirmado());

        Instant fecha = Instant.parse("2026-11-16T10:00:00Z");
        comprobante.marcarAutorizado("<autorizacion/>", "1611202601172203995300110010010000000011234567813", fecha);
        assertEquals(EstadoComprobante.AUTORIZADO, comprobante.getEstado());
        assertEquals("<autorizacion/>", comprobante.getXmlAutorizado());
        assertEquals(fecha, comprobante.getFechaAutorizacion());

        comprobante.adjuntarRide(new byte[] {1, 2, 3});
        assertArrayEquals(new byte[] {1, 2, 3}, comprobante.getRidePdf());

        comprobante.marcarNotificado();
        assertEquals(EstadoComprobante.NOTIFICADO, comprobante.getEstado());
    }

    @Test
    void laFirmaFallidaYElRechazoGuardanElMotivo() {
        Comprobante sinCertificado = borrador();
        sinCertificado.marcarFirmaFallida("La empresa no tiene certificado activo");
        assertEquals(EstadoComprobante.FIRMA_FALLIDA, sinCertificado.getEstado());
        assertEquals("La empresa no tiene certificado activo", sinCertificado.getMotivoRechazo());

        Comprobante rechazado = borrador();
        rechazado.marcarFirmado("<firmado/>");
        rechazado.marcarNoAutorizado("CLAVE ACCESO REGISTRADA");
        assertEquals(EstadoComprobante.NO_AUTORIZADO, rechazado.getEstado());
        assertEquals("CLAVE ACCESO REGISTRADA", rechazado.getMotivoRechazo());
    }

    @Test
    void unaTransicionIlegalFallaSinTocarNada() {
        Comprobante comprobante = borrador();

        assertThrows(IllegalStateException.class,
            () -> comprobante.marcarAutorizado("<autorizacion/>", "123", Instant.now()));

        assertEquals(EstadoComprobante.BORRADOR, comprobante.getEstado());
        assertEquals(null, comprobante.getXmlAutorizado());
    }

    @Test
    void noSeRetrocedeNiSeAutorizaDosVeces() {
        Comprobante comprobante = borrador();
        comprobante.marcarFirmado("<firmado/>");
        comprobante.marcarAutorizado("<a/>", "1", Instant.now());

        assertThrows(IllegalStateException.class, () -> comprobante.marcarFirmado("<otro/>"));
        assertThrows(IllegalStateException.class, () -> comprobante.marcarAutorizado("<b/>", "2", Instant.now()));
        assertEquals("<a/>", comprobante.getXmlAutorizado());
    }

    @Test
    void elRideSoloSeAdjuntaAUnComprobanteAutorizado() {
        assertThrows(IllegalStateException.class, () -> borrador().adjuntarRide(new byte[] {1}));
    }

    @Test
    void consultaSiUnDestinoSaltaPasosDelCiclo() {
        Comprobante comprobante = borrador();
        assertEquals(true, comprobante.esSaltoDePasos(EstadoComprobante.AUTORIZADO));
        assertEquals(false, comprobante.esSaltoDePasos(EstadoComprobante.FIRMADO));
        assertEquals(true, comprobante.puedePasarA(EstadoComprobante.FIRMADO));
    }
}
