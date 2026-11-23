package com.facturacion.orquestacion.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.facturacion.orquestacion.domain.model.entity.SagaInstance;
import com.facturacion.orquestacion.domain.model.enums.SagaState;
import com.facturacion.orquestacion.domain.service.PoliticaReenvioNotificacion.Accion;

/** Qué hace el job de reenvío con cada saga: reenviar, dar por perdido el resultado o esperar. */
class PoliticaReenvioNotificacionTest {

    private static final Instant AHORA = Instant.parse("2026-11-17T20:00:00Z");
    private static final PoliticaReenvioNotificacion.Plazos PLAZOS =
        new PoliticaReenvioNotificacion.Plazos(Duration.ofMinutes(15), Duration.ofMinutes(10), 3);

    private SagaInstance saga(SagaState estado, long minutosQuieta, int intentos, boolean notificacionEsperada) {
        SagaInstance s = SagaInstance.iniciar(UUID.randomUUID(), "1722039953001", "1".repeat(49), "PRUEBAS",
            notificacionEsperada);
        s.setState(estado);
        s.setUpdatedAt(AHORA.minus(Duration.ofMinutes(minutosQuieta)));
        s.setReintentosNotificacion(intentos);
        return s;
    }

    @Test
    void testDeberiaReenviaUnaFallidaQueLlevaQuietaLaEsperaMinimaYTieneIntentos() {
        assertEquals(Accion.REENVIAR, PoliticaReenvioNotificacion.decidir(
            saga(SagaState.NOTIFICACION_FALLIDA, 15, 0, true), AHORA, PLAZOS));
        assertEquals(Accion.REENVIAR, PoliticaReenvioNotificacion.decidir(
            saga(SagaState.NOTIFICACION_FALLIDA, 120, 2, true), AHORA, PLAZOS));
    }

    @Test
    void testDeberiaUnaFallidaRecienFallidaEsperaAntesDeReenviarse() {
        assertEquals(Accion.ESPERAR, PoliticaReenvioNotificacion.decidir(
            saga(SagaState.NOTIFICACION_FALLIDA, 14, 0, true), AHORA, PLAZOS));
    }

    @Test
    void testDeberiaConLosTresIntentosAgotadosYaNoSeReenvia() {
        assertEquals(Accion.ESPERAR, PoliticaReenvioNotificacion.decidir(
            saga(SagaState.NOTIFICACION_FALLIDA, 600, 3, true), AHORA, PLAZOS));
    }

    @Test
    void testDeberiaUnaAutorizadaSinRespuestaTrasElPlazoVuelveANotificacionFallida() {
        assertEquals(Accion.DAR_POR_SIN_RESPUESTA, PoliticaReenvioNotificacion.decidir(
            saga(SagaState.AUTORIZADO, 10, 1, true), AHORA, PLAZOS));
    }

    @Test
    void testDeberiaUnaAutorizadaDentroDelPlazoOSinNotificacionEsperadaNoSeToca() {
        assertEquals(Accion.ESPERAR, PoliticaReenvioNotificacion.decidir(
            saga(SagaState.AUTORIZADO, 9, 0, true), AHORA, PLAZOS));
        assertEquals(Accion.ESPERAR, PoliticaReenvioNotificacion.decidir(
            saga(SagaState.AUTORIZADO, 600, 0, false), AHORA, PLAZOS));
    }

    @Test
    void testDeberiaLosDemasEstadosSeIgnoran() {
        for (SagaState estado : SagaState.values()) {
            if (estado == SagaState.AUTORIZADO || estado == SagaState.NOTIFICACION_FALLIDA) {
                continue;
            }
            assertEquals(Accion.ESPERAR, PoliticaReenvioNotificacion.decidir(saga(estado, 600, 0, true), AHORA, PLAZOS),
                estado.name());
        }
    }
}
