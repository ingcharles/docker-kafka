package com.facturacion.orquestacion.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.facturacion.orquestacion.application.event.NotificacionResultadoEventMessage;
import com.facturacion.orquestacion.domain.model.entity.SagaInstance;
import com.facturacion.orquestacion.domain.model.enums.SagaState;
import com.facturacion.orquestacion.shared.constant.MessagingConstants;

/** El reenvío masivo manda a reenviar solo las sagas en NOTIFICACION_FALLIDA de la empresa, y una sola vez cada una. */
class SagaReenvioMasivoTest {

    private static final String RUC = "1722039953001";

    private final SagaInventarioTest.FakeSagas sagas = new SagaInventarioTest.FakeSagas();
    private final SagaInventarioTest.FakePasos pasos = new SagaInventarioTest.FakePasos();
    private final SagaInventarioTest.FakeSalientes salientes = new SagaInventarioTest.FakeSalientes();
    private final SagaInventarioTest.FakeInbox inbox = new SagaInventarioTest.FakeInbox();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final SagaOrchestratorService servicio =
        new SagaOrchestratorService(sagas, pasos, salientes, inbox, mapper, false);

    private UUID saga(String ruc, SagaState estado) {
        UUID id = UUID.randomUUID();
        SagaInstance saga = SagaInstance.iniciar(id, ruc, "1".repeat(49), "PRUEBAS", true);
        saga.setState(estado);
        sagas.persist(saga).await().atMost(Duration.ofSeconds(5));
        return id;
    }

    private int reenviar(String ruc, int limite) {
        return servicio.reenviarNotificacionesFallidas(ruc, limite).await().atMost(Duration.ofSeconds(5));
    }

    @Test
    void testDeberiaReenviarSoloLasFallidasDeLaEmpresaYPonerlasAEsperarLaNotificacion() {
        UUID fallida1 = saga(RUC, SagaState.NOTIFICACION_FALLIDA);
        UUID fallida2 = saga(RUC, SagaState.NOTIFICACION_FALLIDA);
        UUID notificada = saga(RUC, SagaState.NOTIFICADO);
        UUID deOtraEmpresa = saga("0190072002001", SagaState.NOTIFICACION_FALLIDA);

        int solicitados = reenviar(RUC, 100);

        assertEquals(2, solicitados);
        assertEquals(SagaState.AUTORIZADO, sagas.estado(fallida1));
        assertEquals(SagaState.AUTORIZADO, sagas.estado(fallida2));
        assertEquals(SagaState.NOTIFICADO, sagas.estado(notificada));
        assertEquals(SagaState.NOTIFICACION_FALLIDA, sagas.estado(deOtraEmpresa));
        assertEquals(2, salientes.eventos.size());
        assertTrue(salientes.eventos.stream().allMatch(e -> e.topic().equals(MessagingConstants.COMPROBANTE_COMMANDS_TOPIC)
            && e.payload().contains("\"tipoComando\":\"" + MessagingConstants.COMANDO_REENVIAR_NOTIFICACION + "\"")));
    }

    @Test
    void testDeberiaNoReenviarEnUnaSegundaLlamadaLasQueYaSeMandaron() {
        saga(RUC, SagaState.NOTIFICACION_FALLIDA);

        assertEquals(1, reenviar(RUC, 100));
        assertEquals(0, reenviar(RUC, 100));
        assertEquals(1, salientes.eventos.size());
    }

    @Test
    void testDeberiaAcotarElLoteConElLimiteYReenviarElRestoEnLaSiguienteLlamada() {
        for (int i = 0; i < 3; i++) {
            saga(RUC, SagaState.NOTIFICACION_FALLIDA);
        }

        assertEquals(2, reenviar(RUC, 2));
        assertEquals(1, reenviar(RUC, 2));
        assertEquals(0, reenviar(RUC, 2));
    }

    private void resultadoDeNotificacion(UUID id, String tipo, String motivo) {
        servicio.procesarEventoNotificacion(new NotificacionResultadoEventMessage(UUID.randomUUID(), id, tipo, "x", motivo,
            Instant.now())).await().atMost(Duration.ofSeconds(5));
    }

    @Test
    void testDeberiaPasarLaSagaANotificadoYBorrarElMotivoDelFalloSiElReenvioSaleBien() {
        UUID id = saga(RUC, SagaState.NOTIFICACION_FALLIDA);
        sagas.get(id).setFailureReason("ClosedChannelException");

        resultadoDeNotificacion(id, MessagingConstants.EVENT_TYPE_NOTIFICACION_ENVIADA, null);

        assertEquals(SagaState.NOTIFICADO, sagas.estado(id));
        assertEquals(null, sagas.get(id).getFailureReason());
    }

    @Test
    void testDeberiaVolverANotificacionFallidaSiElReenvioFallaOtraVez() {
        UUID id = saga(RUC, SagaState.AUTORIZADO);

        resultadoDeNotificacion(id, MessagingConstants.EVENT_TYPE_NOTIFICACION_FALLIDA, "socket closed");

        assertEquals(SagaState.NOTIFICACION_FALLIDA, sagas.estado(id));
    }
}
