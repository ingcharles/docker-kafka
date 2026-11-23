package com.facturacion.orquestacion.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.facturacion.orquestacion.domain.model.entity.SagaInstance;
import com.facturacion.orquestacion.domain.model.enums.SagaState;
import com.facturacion.orquestacion.domain.service.PoliticaReintentoSaga.Accion;
import com.facturacion.orquestacion.domain.service.PoliticaReintentoSaga.Plazos;

class PoliticaReintentoSagaTest {

    private static final Instant AHORA = Instant.parse("2026-11-05T12:00:00Z");
    private static final Plazos PLAZOS = new Plazos(Duration.ofMinutes(2), Duration.ofMinutes(15), 3);

    private static SagaInstance saga(SagaState estado, Duration inactiva, int reintentos) {
        SagaInstance saga = SagaInstance.iniciar(UUID.randomUUID(), "1722039953001", "x", "PRUEBAS", true);
        saga.setState(estado);
        saga.setUpdatedAt(AHORA.minus(inactiva));
        saga.setReintentos(reintentos);
        return saga;
    }

    @Test
    void testDeberiaEsperaMientrasNoSePaseElPlazoDelEstado() {
        assertEquals(Accion.ESPERAR, PoliticaReintentoSaga.decidir(saga(SagaState.CREADO, Duration.ofSeconds(119), 0), AHORA, PLAZOS));
        assertEquals(Accion.ESPERAR, PoliticaReintentoSaga.decidir(saga(SagaState.FIRMADO, Duration.ofMinutes(14), 0), AHORA, PLAZOS));
    }

    @Test
    void testDeberiaReintentaCuandoSePasaElPlazoYQuedanReintentos() {
        assertEquals(Accion.REINTENTAR, PoliticaReintentoSaga.decidir(saga(SagaState.CREADO, Duration.ofMinutes(2), 0), AHORA, PLAZOS));
        assertEquals(Accion.REINTENTAR, PoliticaReintentoSaga.decidir(saga(SagaState.FIRMADO, Duration.ofMinutes(16), 2), AHORA, PLAZOS));
    }

    @Test
    void testDeberiaExpiraCuandoSePasaElPlazoYYaNoQuedanReintentos() {
        assertEquals(Accion.EXPIRAR, PoliticaReintentoSaga.decidir(saga(SagaState.CREADO, Duration.ofMinutes(3), 3), AHORA, PLAZOS));
        assertEquals(Accion.EXPIRAR, PoliticaReintentoSaga.decidir(saga(SagaState.FIRMADO, Duration.ofHours(1), 5), AHORA, PLAZOS));
    }

    @Test
    void testDeberiaElPlazoDelSriEsMasLargoQueElDeLaFirma() {
        Duration intermedia = Duration.ofMinutes(5);
        assertEquals(Accion.REINTENTAR, PoliticaReintentoSaga.decidir(saga(SagaState.CREADO, intermedia, 0), AHORA, PLAZOS));
        assertEquals(Accion.ESPERAR, PoliticaReintentoSaga.decidir(saga(SagaState.FIRMADO, intermedia, 0), AHORA, PLAZOS));
    }

    @Test
    void testDeberiaLosEstadosDeInventarioSeReintentanConElPlazoDeLaFirma() {
        assertEquals(Accion.REINTENTAR, PoliticaReintentoSaga.decidir(saga(SagaState.STOCK_PENDIENTE, Duration.ofMinutes(2), 0), AHORA, PLAZOS));
        assertEquals(Accion.REINTENTAR, PoliticaReintentoSaga.decidir(saga(SagaState.STOCK_RESERVADO, Duration.ofMinutes(2), 0), AHORA, PLAZOS));
        assertEquals(Accion.ESPERAR, PoliticaReintentoSaga.decidir(saga(SagaState.STOCK_PENDIENTE, Duration.ofSeconds(30), 0), AHORA, PLAZOS));
    }

    @Test
    void testDeberiaNuncaTocaLosEstadosQueNoSonReintentables() {
        Duration mucho = Duration.ofDays(30);
        for (SagaState estado : SagaState.values()) {
            if (!estado.esReintentable()) {
                assertEquals(Accion.ESPERAR, PoliticaReintentoSaga.decidir(saga(estado, mucho, 0), AHORA, PLAZOS), estado.name());
            }
        }
    }

    @Test
    void testDeberiaSoloLosEstadosQueEsperanRespuestaDeOtroServicioSeReintentan() {
        assertEquals(List.of(SagaState.STOCK_PENDIENTE, SagaState.STOCK_RESERVADO, SagaState.CREADO, SagaState.FIRMADO),
            SagaState.reintentables());
    }

    @Test
    void testDeberiaUnaSagaSinCorreoTerminaEnAutorizadoSinNotificacionYElWatchdogNoLaToca() {
        assertTrue(SagaState.AUTORIZADO_SIN_NOTIFICACION.esTerminal());
        assertFalse(SagaState.AUTORIZADO_SIN_NOTIFICACION.esReintentable());
        SagaInstance sinCorreo = SagaInstance.iniciar(UUID.randomUUID(), "1722039953001", "x", "PRUEBAS", false);
        assertFalse(sinCorreo.isNotificacionEsperada());
        assertTrue(SagaInstance.iniciar(UUID.randomUUID(), "1722039953001", "x", "PRUEBAS", true).isNotificacionEsperada());
        sinCorreo.setState(SagaState.AUTORIZADO_SIN_NOTIFICACION);
        sinCorreo.setUpdatedAt(AHORA.minus(Duration.ofDays(30)));
        assertEquals(Accion.ESPERAR, PoliticaReintentoSaga.decidir(sinCorreo, AHORA, PLAZOS));
    }

    @Test
    void testDeberiaLosEstadosTerminalesNoSonReintentablesYAutorizadoNoEsTerminalNiReintentable() {
        for (SagaState estado : SagaState.values()) {
            assertFalse(estado.esTerminal() && estado.esReintentable(), estado.name());
        }
        assertFalse(SagaState.AUTORIZADO.esTerminal());
        assertFalse(SagaState.AUTORIZADO.esReintentable());
        assertTrue(SagaState.EXPIRADA.esTerminal());
    }
}
