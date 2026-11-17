package com.facturacion.orquestacion.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.facturacion.orquestacion.application.event.NotificacionResultadoEventMessage;
import com.facturacion.orquestacion.domain.model.entity.SagaInstance;
import com.facturacion.orquestacion.domain.model.enums.SagaState;
import com.facturacion.orquestacion.domain.service.PoliticaReenvioNotificacion;
import com.facturacion.orquestacion.domain.service.PoliticaReenvioNotificacion.Accion;
import com.facturacion.orquestacion.shared.constant.MessagingConstants;
import com.facturacion.orquestacion.shared.error.CodigoError;

/**
 * El reenvío automático de correos: reenvía hasta el tope y con espera entre intentos, da por perdido un resultado que no
 * llega y deja el flujo terminando en NOTIFICADO o en NOTIFICACION_FALLIDA con los intentos agotados.
 */
class SagaReenvioAutomaticoTest {

    private static final String RUC = "1722039953001";
    private static final PoliticaReenvioNotificacion.Plazos PLAZOS =
        new PoliticaReenvioNotificacion.Plazos(Duration.ofMinutes(15), Duration.ofMinutes(10), 3);

    private final SagaInventarioTest.FakeSagas sagas = new SagaInventarioTest.FakeSagas();
    private final SagaInventarioTest.FakePasos pasos = new SagaInventarioTest.FakePasos();
    private final SagaInventarioTest.FakeSalientes salientes = new SagaInventarioTest.FakeSalientes();
    private final SagaInventarioTest.FakeInbox inbox = new SagaInventarioTest.FakeInbox();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final SagaOrchestratorService servicio =
        new SagaOrchestratorService(sagas, pasos, salientes, inbox, mapper, false);

    private UUID saga(SagaState estado, long minutosQuieta, int intentos) {
        UUID id = UUID.randomUUID();
        SagaInstance saga = SagaInstance.iniciar(id, RUC, "1".repeat(49), "PRUEBAS", true);
        saga.setState(estado);
        saga.setUpdatedAt(Instant.now().minus(Duration.ofMinutes(minutosQuieta)));
        saga.setReintentosNotificacion(intentos);
        sagas.persist(saga).await().atMost(Duration.ofSeconds(5));
        return id;
    }

    private Accion atender(UUID id) {
        return servicio.reenviarNotificacionAutomaticamente(id, PLAZOS).await().atMost(Duration.ofSeconds(5));
    }

    private List<UUID> pendientes(int lote) {
        return servicio.idsParaReenvioAutomatico(PLAZOS, lote).await().atMost(Duration.ofSeconds(5));
    }

    private void resultadoDeNotificacion(UUID id, String tipo, String motivo) {
        servicio.procesarEventoNotificacion(new NotificacionResultadoEventMessage(UUID.randomUUID(), id, tipo, "x", motivo,
            Instant.now())).await().atMost(Duration.ofSeconds(5));
    }

    private void envejecer(UUID id, long minutos) {
        sagas.get(id).setUpdatedAt(Instant.now().minus(Duration.ofMinutes(minutos)));
    }

    @Test
    void reenviaUnaFallidaVieja_cuentaElIntentoYPideElReenvioPorElOutbox() {
        UUID id = saga(SagaState.NOTIFICACION_FALLIDA, 30, 0);

        assertEquals(Accion.REENVIAR, atender(id));

        assertEquals(SagaState.AUTORIZADO, sagas.estado(id));
        assertEquals(1, sagas.get(id).getReintentosNotificacion());
        assertEquals(1, salientes.eventos.size());
        assertEquals(MessagingConstants.COMPROBANTE_COMMANDS_TOPIC, salientes.eventos.get(0).topic());
        assertTrue(salientes.eventos.get(0).payload()
            .contains("\"tipoComando\":\"" + MessagingConstants.COMANDO_REENVIAR_NOTIFICACION + "\""));
        assertTrue(pasos.hayNota(CodigoError.REENVIO_AUTOMATICO_NOTIFICACION.codigo()));
    }

    @Test
    void unaFallidaRecienFallidaNoSeReenvia() {
        UUID id = saga(SagaState.NOTIFICACION_FALLIDA, 1, 0);

        assertEquals(Accion.ESPERAR, atender(id));

        assertEquals(SagaState.NOTIFICACION_FALLIDA, sagas.estado(id));
        assertTrue(salientes.eventos.isEmpty());
    }

    @Test
    void dosEjecucionesSeguidasNoDuplicanElEnvio() {
        UUID id = saga(SagaState.NOTIFICACION_FALLIDA, 30, 0);

        assertEquals(Accion.REENVIAR, atender(id));
        assertEquals(Accion.ESPERAR, atender(id));

        assertEquals(1, salientes.eventos.size());
        assertEquals(1, sagas.get(id).getReintentosNotificacion());
    }

    @Test
    void unaAutorizadaQueNuncaRecibioResultadoVuelveANotificacionFallidaSinGastarIntento() {
        UUID id = saga(SagaState.AUTORIZADO, 30, 1);

        assertEquals(Accion.DAR_POR_SIN_RESPUESTA, atender(id));

        assertEquals(SagaState.NOTIFICACION_FALLIDA, sagas.estado(id));
        assertEquals(1, sagas.get(id).getReintentosNotificacion());
        assertNotNull(sagas.get(id).getFailureReason());
        assertTrue(salientes.eventos.isEmpty());
        assertTrue(pasos.hayNota(CodigoError.NOTIFICACION_SIN_RESPUESTA.codigo()));
    }

    @Test
    void conTresIntentosAgotadosNoSeReenviaMasYNoOcupaLugarEnElLote() {
        UUID agotada1 = saga(SagaState.NOTIFICACION_FALLIDA, 900, 3);
        UUID agotada2 = saga(SagaState.NOTIFICACION_FALLIDA, 800, 3);
        UUID elegible1 = saga(SagaState.NOTIFICACION_FALLIDA, 60, 0);
        UUID elegible2 = saga(SagaState.NOTIFICACION_FALLIDA, 50, 1);

        List<UUID> ids = pendientes(2);

        assertEquals(List.of(elegible1, elegible2), ids);
        assertEquals(Accion.ESPERAR, atender(agotada1));
        assertEquals(Accion.ESPERAR, atender(agotada2));
        assertEquals(SagaState.NOTIFICACION_FALLIDA, sagas.estado(agotada1));
    }

    @Test
    void elLoteEsDeVeinteYElRestoQuedaParaElSiguienteCiclo() {
        for (int i = 0; i < 25; i++) {
            saga(SagaState.NOTIFICACION_FALLIDA, 60, 0);
        }

        List<UUID> primerLote = pendientes(20);
        primerLote.forEach(this::atender);

        assertEquals(20, primerLote.size());
        assertEquals(20, salientes.eventos.size());
        assertEquals(5, pendientes(20).size());
    }

    @Test
    void siElEnvioSaleBienLaSagaTerminaEnNotificado() {
        UUID id = saga(SagaState.NOTIFICACION_FALLIDA, 30, 0);
        sagas.get(id).setFailureReason("ClosedChannelException");

        atender(id);
        resultadoDeNotificacion(id, MessagingConstants.EVENT_TYPE_NOTIFICACION_ENVIADA, null);

        assertEquals(SagaState.NOTIFICADO, sagas.estado(id));
        assertEquals(null, sagas.get(id).getFailureReason());
    }

    @Test
    void siElEnvioFallaSiempreSeIntentaTresVecesYLaSagaQuedaVisibleEnNotificacionFallida() {
        UUID id = saga(SagaState.NOTIFICACION_FALLIDA, 30, 0);

        for (int intento = 1; intento <= 3; intento++) {
            assertEquals(Accion.REENVIAR, atender(id), "intento " + intento);
            resultadoDeNotificacion(id, MessagingConstants.EVENT_TYPE_NOTIFICACION_FALLIDA, "ClosedChannelException");
            assertEquals(SagaState.NOTIFICACION_FALLIDA, sagas.estado(id));
            envejecer(id, 30);
        }

        assertEquals(Accion.ESPERAR, atender(id));
        assertTrue(pendientes(20).isEmpty());
        assertEquals(SagaState.NOTIFICACION_FALLIDA, sagas.estado(id));
        assertEquals(3, sagas.get(id).getReintentosNotificacion());
        assertEquals(3, salientes.eventos.size());
    }

    @Test
    void elReenvioManualSigueFuncionandoConIntentosAgotadosYNoCuentaComoAutomatico() {
        UUID id = saga(SagaState.NOTIFICACION_FALLIDA, 900, 3);

        int solicitados = servicio.reenviarNotificacionesFallidas(RUC, 100).await().atMost(Duration.ofSeconds(5));

        assertEquals(1, solicitados);
        assertEquals(SagaState.AUTORIZADO, sagas.estado(id));
        assertEquals(3, sagas.get(id).getReintentosNotificacion());
    }
}
