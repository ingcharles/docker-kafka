package com.facturacion.orquestacion.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.facturacion.commons.paging.PageResult;
import com.facturacion.orquestacion.application.event.ComprobanteEventMessage;
import com.facturacion.orquestacion.application.event.ComprobanteFirmadoEventMessage;
import com.facturacion.orquestacion.application.event.ItemCantidad;
import com.facturacion.orquestacion.application.event.SriResultadoEventMessage;
import com.facturacion.orquestacion.application.event.StockEventMessage;
import com.facturacion.orquestacion.domain.model.entity.SagaInstance;
import com.facturacion.orquestacion.domain.model.entity.SagaStep;
import com.facturacion.orquestacion.domain.model.enums.SagaState;
import com.facturacion.orquestacion.domain.port.EventoSaliente;
import com.facturacion.orquestacion.domain.port.EventosSalientes;
import com.facturacion.orquestacion.domain.port.MensajesProcesados;
import com.facturacion.orquestacion.domain.repository.SagaInstanceRepository;
import com.facturacion.orquestacion.domain.repository.SagaStepRepository;
import com.facturacion.orquestacion.shared.constant.MessagingConstants;

/**
 * El descuento de inventario lo decide una bandera: apagada, la saga es la de siempre; encendida, reserva el stock
 * antes de firmar, lo confirma al autorizar y lo libera si el SRI rechaza o la firma falla.
 */
class SagaInventarioTest {

    private final FakeSagas sagas = new FakeSagas();
    private final FakePasos pasos = new FakePasos();
    private final FakeSalientes salientes = new FakeSalientes();
    private final FakeInbox inbox = new FakeInbox();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final UUID item = UUID.randomUUID();

    private SagaOrchestratorService servicio(boolean descuentoInventario) {
        return new SagaOrchestratorService(sagas, pasos, salientes, inbox, mapper, descuentoInventario);
    }

    private ComprobanteEventMessage creado(UUID id, String tipo, List<ItemCantidad> items) {
        return new ComprobanteEventMessage(UUID.randomUUID(), id, MessagingConstants.EVENT_TYPE_COMPROBANTE_CREADO,
            "1722039953001", "1".repeat(49), "PRUEBAS", "<factura/>", true, tipo, items, Instant.now());
    }

    private List<ItemCantidad> unItem() {
        return List.of(new ItemCantidad(item, new BigDecimal("2")));
    }

    private static <T> T esperar(Uni<T> uni) {
        return uni.await().atMost(Duration.ofSeconds(5));
    }

    private void stock(SagaOrchestratorService servicio, UUID id, String tipo, List<UUID> faltantes) {
        esperar(servicio.procesarEventoStock(new StockEventMessage(UUID.randomUUID(), id, tipo, faltantes, Instant.now())));
    }

    private void firmado(SagaOrchestratorService servicio, UUID id) {
        esperar(servicio.procesarEventoFirma(new ComprobanteFirmadoEventMessage(UUID.randomUUID(), id,
            MessagingConstants.EVENT_TYPE_COMPROBANTE_FIRMADO, "<firmado/>", null, Instant.now())));
    }

    private void sri(SagaOrchestratorService servicio, UUID id, String tipo) {
        esperar(servicio.procesarEventoSri(new SriResultadoEventMessage(UUID.randomUUID(), id, "1".repeat(49), tipo,
            "<autorizacion/>", "123", Instant.now(), "rechazado", Instant.now())));
    }

    private boolean salioComando(String tipoComando) {
        return salientes.eventos.stream().anyMatch(e -> e.payload().contains("\"tipoComando\":\"" + tipoComando + "\""));
    }

    @Test
    void testDeberiaMantenerLaSagaDeSiempreSinTocarInventarioConLaBanderaApagada() {
        UUID id = UUID.randomUUID();

        esperar(servicio(false).procesarEventoComprobante(creado(id, "FACTURA", unItem())));

        assertEquals(SagaState.CREADO, sagas.estado(id));
        assertFalse(sagas.get(id).isUsaStock());
        assertEquals(List.of(MessagingConstants.FIRMA_COMMANDS_TOPIC), salientes.topicos());
    }

    @Test
    void testDeberiaReservarElStockAntesDeFirmarConLaBanderaEncendida() {
        UUID id = UUID.randomUUID();

        esperar(servicio(true).procesarEventoComprobante(creado(id, "FACTURA", unItem())));

        assertEquals(SagaState.STOCK_PENDIENTE, sagas.estado(id));
        assertTrue(sagas.get(id).isUsaStock());
        assertEquals(List.of(MessagingConstants.INVENTARIO_COMMANDS_TOPIC), salientes.topicos());
        assertTrue(salioComando("RESERVAR"));
        assertTrue(salientes.eventos.get(0).payload().contains(item.toString()));
        assertTrue(pasos.hayNota("FIRMA_PENDIENTE"), "el comando de firma queda esperando la reserva");
    }

    @Test
    void testDeberiaDescontarStockSoloEnLasFacturasConItemsDeInventario() {
        UUID notaCredito = UUID.randomUUID();
        UUID facturaSinItems = UUID.randomUUID();
        UUID facturaConItemsNulos = UUID.randomUUID();
        SagaOrchestratorService servicio = servicio(true);

        esperar(servicio.procesarEventoComprobante(creado(notaCredito, "NOTA_CREDITO", unItem())));
        esperar(servicio.procesarEventoComprobante(creado(facturaSinItems, "FACTURA", List.of())));
        esperar(servicio.procesarEventoComprobante(creado(facturaConItemsNulos, "FACTURA", null)));

        for (UUID id : List.of(notaCredito, facturaSinItems, facturaConItemsNulos)) {
            assertEquals(SagaState.CREADO, sagas.estado(id));
            assertFalse(sagas.get(id).isUsaStock());
        }
        assertEquals(List.of(MessagingConstants.FIRMA_COMMANDS_TOPIC, MessagingConstants.FIRMA_COMMANDS_TOPIC,
            MessagingConstants.FIRMA_COMMANDS_TOPIC), salientes.topicos());
    }

    @Test
    void testDeberiaEmitirElComandoDeFirmaGuardadoCuandoElStockQuedaReservado() {
        UUID id = UUID.randomUUID();
        SagaOrchestratorService servicio = servicio(true);
        esperar(servicio.procesarEventoComprobante(creado(id, "FACTURA", unItem())));
        salientes.eventos.clear();

        stock(servicio, id, MessagingConstants.EVENT_TYPE_STOCK_RESERVADO, List.of());

        assertEquals(SagaState.STOCK_RESERVADO, sagas.estado(id));
        assertEquals(List.of(MessagingConstants.FIRMA_COMMANDS_TOPIC), salientes.topicos());
        assertTrue(salientes.eventos.get(0).payload().contains("<factura/>"));
    }

    @Test
    void testDeberiaTerminarLaSagaYMarcarElComprobanteSinFirmarNiLiberarSiNoHayStock() {
        UUID id = UUID.randomUUID();
        SagaOrchestratorService servicio = servicio(true);
        esperar(servicio.procesarEventoComprobante(creado(id, "FACTURA", unItem())));
        salientes.eventos.clear();

        stock(servicio, id, MessagingConstants.EVENT_TYPE_STOCK_NO_DISPONIBLE, List.of(item));

        assertEquals(SagaState.STOCK_NO_DISPONIBLE, sagas.estado(id));
        assertTrue(sagas.estado(id).esTerminal());
        assertEquals(List.of(MessagingConstants.COMPROBANTE_COMMANDS_TOPIC), salientes.topicos());
        String payload = salientes.eventos.get(0).payload();
        assertTrue(payload.contains(MessagingConstants.COMANDO_MARCAR_STOCK_INSUFICIENTE));
        assertTrue(payload.contains(item.toString()));
    }

    @Test
    void testDeberiaConfirmarElStockAlAutorizarElSri() {
        UUID id = UUID.randomUUID();
        SagaOrchestratorService servicio = servicio(true);
        esperar(servicio.procesarEventoComprobante(creado(id, "FACTURA", unItem())));
        stock(servicio, id, MessagingConstants.EVENT_TYPE_STOCK_RESERVADO, List.of());
        firmado(servicio, id);
        salientes.eventos.clear();

        sri(servicio, id, MessagingConstants.EVENT_TYPE_SRI_AUTORIZADO);

        assertEquals(SagaState.AUTORIZADO, sagas.estado(id));
        assertTrue(salientes.topicos().contains(MessagingConstants.INVENTARIO_COMMANDS_TOPIC));
        assertTrue(salioComando("CONFIRMAR"));
    }

    @Test
    void testDeberiaLiberarElStockSiElSriRechazaOLaFirmaFalla() {
        UUID rechazada = UUID.randomUUID();
        UUID sinFirma = UUID.randomUUID();
        SagaOrchestratorService servicio = servicio(true);
        for (UUID id : List.of(rechazada, sinFirma)) {
            esperar(servicio.procesarEventoComprobante(creado(id, "FACTURA", unItem())));
            stock(servicio, id, MessagingConstants.EVENT_TYPE_STOCK_RESERVADO, List.of());
        }
        firmado(servicio, rechazada);
        salientes.eventos.clear();

        sri(servicio, rechazada, MessagingConstants.EVENT_TYPE_SRI_NO_AUTORIZADO);
        assertTrue(salioComando("LIBERAR"));

        salientes.eventos.clear();
        esperar(servicio.procesarEventoFirma(new ComprobanteFirmadoEventMessage(UUID.randomUUID(), sinFirma,
            MessagingConstants.EVENT_TYPE_FIRMA_FALLIDA, null, "sin certificado", Instant.now())));
        assertEquals(SagaState.FIRMA_FALLIDA, sagas.estado(sinFirma));
        assertTrue(salioComando("LIBERAR"));
    }

    @Test
    void testDeberiaNoConfirmarNiLiberarStockEnUnaSagaSinInventario() {
        UUID id = UUID.randomUUID();
        SagaOrchestratorService servicio = servicio(false);
        esperar(servicio.procesarEventoComprobante(creado(id, "FACTURA", unItem())));
        firmado(servicio, id);
        salientes.eventos.clear();

        sri(servicio, id, MessagingConstants.EVENT_TYPE_SRI_AUTORIZADO);

        assertFalse(salientes.topicos().contains(MessagingConstants.INVENTARIO_COMMANDS_TOPIC));
    }

    @Test
    void testDeberiaEvaluarLaBanderaAlCrearLaSagaYTerminarUnaEnCursoComoEmpezo() {
        UUID id = UUID.randomUUID();
        esperar(servicio(true).procesarEventoComprobante(creado(id, "FACTURA", unItem())));
        stock(servicio(false), id, MessagingConstants.EVENT_TYPE_STOCK_RESERVADO, List.of());
        firmado(servicio(false), id);
        salientes.eventos.clear();

        sri(servicio(false), id, MessagingConstants.EVENT_TYPE_SRI_AUTORIZADO);

        assertTrue(salioComando("CONFIRMAR"),
            "la saga nació con inventario: debe confirmar aunque la bandera ya esté apagada");
    }

    @Test
    void testDeberiaNoAplicarDosVecesUnEventoRepetido() {
        UUID id = UUID.randomUUID();
        SagaOrchestratorService servicio = servicio(true);
        ComprobanteEventMessage evento = creado(id, "FACTURA", unItem());

        ResultadoEvento primero = esperar(servicio.procesarEventoComprobante(evento));
        ResultadoEvento repetido = esperar(servicio.procesarEventoComprobante(evento));

        assertEquals(ResultadoEvento.APLICADO, primero);
        assertEquals(ResultadoEvento.DUPLICADO, repetido);
        assertEquals(1, salientes.eventos.size());
    }

    @Test
    void testDeberiaIgnorarPorSuTipoUnEventoQueNoMueveLaSaga() {
        ComprobanteEventMessage autorizado = new ComprobanteEventMessage(UUID.randomUUID(), UUID.randomUUID(),
            MessagingConstants.EVENT_TYPE_COMPROBANTE_AUTORIZADO, "1722039953001", "1".repeat(49), "PRUEBAS", null,
            true, "FACTURA", null, Instant.now());

        assertEquals(ResultadoEvento.IGNORADO_TIPO, esperar(servicio(true).procesarEventoComprobante(autorizado)));
        assertTrue(salientes.eventos.isEmpty());
    }

    @Test
    void testDeberiaIgnorarUnEventoFueraDeEstadoDejandoConstancia() {
        UUID id = UUID.randomUUID();
        SagaOrchestratorService servicio = servicio(true);
        esperar(servicio.procesarEventoComprobante(creado(id, "FACTURA", unItem())));
        stock(servicio, id, MessagingConstants.EVENT_TYPE_STOCK_RESERVADO, List.of());
        salientes.eventos.clear();

        // Un segundo StockReservado (otro eventId): la saga ya salió de STOCK_PENDIENTE, no se firma dos veces.
        ResultadoEvento resultado = esperar(servicio.procesarEventoStock(new StockEventMessage(UUID.randomUUID(), id,
            MessagingConstants.EVENT_TYPE_STOCK_RESERVADO, List.of(), Instant.now())));

        assertEquals(ResultadoEvento.IGNORADO_ESTADO_NO_ESPERADO, resultado);
        assertTrue(salientes.eventos.isEmpty());
        assertEquals(SagaState.STOCK_RESERVADO, sagas.estado(id));
    }

    @Test
    void testDeberiaIgnorarPorSuTipoUnEventoInformativoDeInventario() {
        UUID id = UUID.randomUUID();
        SagaOrchestratorService servicio = servicio(true);
        esperar(servicio.procesarEventoComprobante(creado(id, "FACTURA", unItem())));

        ResultadoEvento resultado = esperar(servicio.procesarEventoStock(new StockEventMessage(UUID.randomUUID(), id,
            "StockConfirmado", List.of(), Instant.now())));

        assertEquals(ResultadoEvento.IGNORADO_TIPO, resultado);
        assertEquals(SagaState.STOCK_PENDIENTE, sagas.estado(id));
    }

    // ---- dobles en memoria ----

    static final class FakeSagas implements SagaInstanceRepository {
        final Map<UUID, SagaInstance> datos = new HashMap<>();

        SagaInstance get(UUID id) {
            return datos.get(id);
        }

        SagaState estado(UUID id) {
            return datos.get(id).getState();
        }

        @Override
        public Uni<SagaInstance> persist(SagaInstance instance) {
            datos.put(instance.getId(), instance);
            return Uni.createFrom().item(instance);
        }

        @Override
        public Uni<SagaInstance> findById(UUID id) {
            return Uni.createFrom().item(datos.get(id));
        }

        @Override
        public Uni<PageResult<SagaInstance>> findPaged(int page, int limit, SagaState state, String empresaRuc) {
            List<SagaInstance> items = datos.values().stream()
                .filter(s -> s.getState() == state && (empresaRuc == null || empresaRuc.equals(s.getEmpresaRuc())))
                .limit(limit).toList();
            return Uni.createFrom().item(new PageResult<>(items, items.size()));
        }

        @Override
        public Uni<SagaInstance> transicionarEstado(UUID id, SagaState to, String motivo, SagaState... permitidos) {
            SagaInstance saga = datos.get(id);
            if (saga == null || !List.of(permitidos).contains(saga.getState())) {
                return Uni.createFrom().nullItem();
            }
            saga.setState(to);
            if (motivo != null) {
                saga.setFailureReason(motivo);
            }
            return Uni.createFrom().item(saga);
        }

        @Override
        public Uni<SagaInstance> transicionarEstadoLimpiandoMotivo(UUID id, SagaState to, SagaState... permitidos) {
            return transicionarEstado(id, to, null, permitidos).invoke(s -> {
                if (s != null) {
                    s.setFailureReason(null);
                }
            });
        }

        @Override
        public Uni<List<SagaInstance>> findByEstados(List<SagaState> estados, int limite) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Uni<SagaInstance> registrarReintento(UUID id, SagaState estadoActual) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Uni<List<SagaInstance>> findPendientesDeNotificacion(Instant fallidasAntesDe,
                Instant sinRespuestaAntesDe, int maxIntentos, int limite) {
            List<SagaInstance> items = datos.values().stream()
                .filter(s -> (s.getState() == SagaState.NOTIFICACION_FALLIDA && s.getReintentosNotificacion() < maxIntentos
                        && !s.getUpdatedAt().isAfter(fallidasAntesDe))
                    || (s.getState() == SagaState.AUTORIZADO && s.isNotificacionEsperada()
                        && !s.getUpdatedAt().isAfter(sinRespuestaAntesDe)))
                .sorted(Comparator.comparing(SagaInstance::getUpdatedAt))
                .limit(limite).toList();
            return Uni.createFrom().item(items);
        }

        @Override
        public Uni<SagaInstance> registrarReenvioAutomatico(UUID id, int maxIntentos) {
            SagaInstance saga = datos.get(id);
            if (saga == null || saga.getState() != SagaState.NOTIFICACION_FALLIDA
                    || saga.getReintentosNotificacion() >= maxIntentos) {
                return Uni.createFrom().nullItem();
            }
            saga.setState(SagaState.AUTORIZADO);
            saga.setReintentosNotificacion(saga.getReintentosNotificacion() + 1);
            saga.setUpdatedAt(Instant.now());
            return Uni.createFrom().item(saga);
        }
    }

    static final class FakePasos implements SagaStepRepository {
        final List<SagaStep> pasos = new ArrayList<>();

        boolean hayNota(String codigo) {
            return pasos.stream().anyMatch(p -> "NOTA".equals(p.getStatus()) && codigo.equals(p.getStepName()));
        }

        @Override
        public Uni<SagaStep> persist(SagaStep step) {
            pasos.add(step);
            return Uni.createFrom().item(step);
        }

        @Override
        public Uni<List<SagaStep>> findBySagaId(UUID sagaId) {
            return Uni.createFrom().item(pasos.stream().filter(p -> p.getSagaId().equals(sagaId)).toList());
        }

        @Override
        public Uni<SagaStep> findLastSent(UUID sagaId) {
            return Uni.createFrom().item(pasos.stream()
                .filter(p -> p.getSagaId().equals(sagaId) && "SENT".equals(p.getStatus()))
                .reduce((primero, segundo) -> segundo).orElse(null));
        }

        @Override
        public Uni<SagaStep> findUltimaNota(UUID sagaId, String codigo) {
            return Uni.createFrom().item(pasos.stream()
                .filter(p -> p.getSagaId().equals(sagaId) && "NOTA".equals(p.getStatus())
                    && codigo.equals(p.getStepName()))
                .reduce((primero, segundo) -> segundo).orElse(null));
        }
    }

    static final class FakeSalientes implements EventosSalientes {
        final List<EventoSaliente> eventos = new ArrayList<>();

        List<String> topicos() {
            return eventos.stream().map(EventoSaliente::topic).toList();
        }

        @Override
        public Uni<Void> registrar(EventoSaliente evento) {
            eventos.add(evento);
            return Uni.createFrom().voidItem();
        }
    }

    static final class FakeInbox implements MensajesProcesados {
        final Set<UUID> vistos = new HashSet<>();

        @Override
        public Uni<Boolean> esMensajeProcesado(UUID mensajeId) {
            return Uni.createFrom().item(vistos.contains(mensajeId));
        }

        @Override
        public Uni<Void> marcarProcesado(UUID mensajeId) {
            vistos.add(mensajeId);
            return Uni.createFrom().voidItem();
        }
    }
}
