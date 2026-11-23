package com.facturacion.comprobantes.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;

import com.facturacion.commons.exception.ConflictoException;
import com.facturacion.comprobantes.application.event.ActualizarComprobanteCommand;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.enums.EstadoComprobante;
import com.facturacion.comprobantes.domain.repository.ComprobanteRepository;
import com.facturacion.comprobantes.shared.constant.MessagingConstants;
import com.facturacion.comprobantes.shared.error.CodigoError;

/** aplicarComando dice qué hizo: aplicó el cambio o lo ignoró por repetido; y un salto de pasos falla, no se calla. */
class ActualizacionComprobanteResultadoTest {

    private final UUID id = UUID.randomUUID();
    private final List<Comprobante> guardados = new ArrayList<>();
    private Comprobante comprobante;

    /** Repositorio mínimo: solo findById y update; cualquier otra operación sería un error de la prueba. */
    private ActualizacionComprobanteService servicio(EstadoComprobante estadoInicial) {
        comprobante = new Comprobante();
        comprobante.setId(id);
        comprobante.setEstado(estadoInicial);
        ComprobanteRepository repositorio = (ComprobanteRepository) Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[] {ComprobanteRepository.class}, (proxy, metodo, args) -> {
                return switch (metodo.getName()) {
                    case "findById" -> Uni.createFrom().item(comprobante);
                    case "update" -> {
                        guardados.add((Comprobante) args[0]);
                        yield Uni.createFrom().item((Comprobante) args[0]);
                    }
                    default -> throw new UnsupportedOperationException(metodo.getName());
                };
            });
        return new ActualizacionComprobanteService(repositorio, null, null, null, null, null);
    }

    private ActualizarComprobanteCommand comando(String tipo) {
        return new ActualizarComprobanteCommand(UUID.randomUUID(), id, tipo, "<firmado/>", null, null, null,
            "motivo", Instant.now());
    }

    private static <T> T esperar(Uni<T> uni) {
        return uni.await().atMost(Duration.ofSeconds(5));
    }

    @Test
    void testDeberiaAplicarYGuardarUnCambioDeEstadoValido() {
        ActualizacionComprobanteService servicio = servicio(EstadoComprobante.BORRADOR);

        ResultadoComando resultado = esperar(servicio.aplicarComando(comando(MessagingConstants.COMANDO_MARCAR_FIRMADO)));

        assertEquals(ResultadoComando.APLICADO, resultado);
        assertEquals(EstadoComprobante.FIRMADO, comprobante.getEstado());
        assertEquals(1, guardados.size());
    }

    @Test
    void testDeberiaIgnorarUnComandoRepetidoSinTocarNada() {
        ActualizacionComprobanteService servicio = servicio(EstadoComprobante.FIRMADO);

        ResultadoComando resultado = esperar(servicio.aplicarComando(comando(MessagingConstants.COMANDO_MARCAR_FIRMADO)));

        assertEquals(ResultadoComando.IGNORADO_TRANSICION_NO_PERMITIDA, resultado);
        assertEquals(EstadoComprobante.FIRMADO, comprobante.getEstado());
        assertEquals(0, guardados.size());
    }

    @Test
    void testDeberiaFallarEnVezDeCallarseSiElComandoSaltaPasosDelCiclo() {
        ActualizacionComprobanteService servicio = servicio(EstadoComprobante.BORRADOR);

        CompletionException e = assertThrows(CompletionException.class, () -> servicio
            .aplicarComando(comando(MessagingConstants.COMANDO_MARCAR_AUTORIZADO)).subscribeAsCompletionStage().join());

        assertEquals(ConflictoException.class, e.getCause().getClass());
        assertEquals(CodigoError.COMANDO_FUERA_DE_ORDEN, ((ConflictoException) e.getCause()).getError());
        assertEquals(EstadoComprobante.BORRADOR, comprobante.getEstado());
    }

    @Test
    void testDeberiaDejarElComprobanteEnEstadoTerminalConElMotivoSiNoHayStock() {
        ActualizacionComprobanteService servicio = servicio(EstadoComprobante.BORRADOR);

        ResultadoComando resultado = esperar(servicio.aplicarComando(
            comando(MessagingConstants.COMANDO_MARCAR_STOCK_INSUFICIENTE)));

        assertEquals(ResultadoComando.APLICADO, resultado);
        assertEquals(EstadoComprobante.STOCK_INSUFICIENTE, comprobante.getEstado());
        assertEquals("motivo", comprobante.getMotivoRechazo());
    }
}
