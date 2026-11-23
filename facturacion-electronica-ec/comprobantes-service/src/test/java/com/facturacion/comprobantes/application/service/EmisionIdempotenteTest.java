package com.facturacion.comprobantes.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;

import com.facturacion.commons.exception.ApiException;
import com.facturacion.comprobantes.application.dto.ComprobanteResponse;
import com.facturacion.comprobantes.application.dto.ResultadoEmision;
import com.facturacion.comprobantes.application.mapper.ComprobanteResponseMapperImpl;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.model.vo.RegistroIdempotencia;
import com.facturacion.comprobantes.domain.port.CalculadorHuella;
import com.facturacion.comprobantes.domain.repository.ComprobanteRepository;
import com.facturacion.comprobantes.domain.repository.IdempotenciaRepository;
import com.facturacion.comprobantes.shared.error.CodigoError;

class EmisionIdempotenteTest {

    private static final String RUC = "1722039953001";
    private static final String CLAVE = "pedido-1234-intento";
    private static final Duration RETENCION = Duration.ofDays(7);

    /** Guarda las claves en un mapa, como lo haría la tabla. */
    private static final class IdempotenciaEnMemoria implements IdempotenciaRepository {
        final Map<String, RegistroIdempotencia> filas = new HashMap<>();
        Instant limitePurga;

        @Override
        public Uni<Boolean> reservar(String ruc, String clave, String huella) {
            return Uni.createFrom().item(filas.putIfAbsent(ruc + clave, new RegistroIdempotencia(huella, null)) == null);
        }

        @Override
        public Uni<RegistroIdempotencia> buscar(String ruc, String clave) {
            return Uni.createFrom().item(filas.get(ruc + clave));
        }

        @Override
        public Uni<Void> asociarComprobante(String ruc, String clave, UUID comprobanteId) {
            filas.computeIfPresent(ruc + clave, (k, fila) -> new RegistroIdempotencia(fila.huella(), comprobanteId));
            return Uni.createFrom().voidItem();
        }

        @Override
        public Uni<Long> purgarCreadasAntesDe(Instant limite) {
            limitePurga = limite;
            return Uni.createFrom().item(3L);
        }
    }

    private IdempotenciaEnMemoria idempotencia;
    private final Map<UUID, Comprobante> comprobantes = new HashMap<>();
    private final AtomicInteger emisiones = new AtomicInteger();

    @BeforeEach
    void prepararDobles() {
        idempotencia = new IdempotenciaEnMemoria();
        comprobantes.clear();
        emisiones.set(0);
    }

    private EmisionIdempotente crearServicio(boolean obligatoria) {
        ComprobanteRepository repositorio = (ComprobanteRepository) Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[] {ComprobanteRepository.class},
            (proxy, metodo, args) -> Uni.createFrom().item(comprobantes.get((UUID) args[0])));
        // Huella de prueba: el texto del contenido, sin calcular nada.
        CalculadorHuella huella = solicitud -> String.valueOf(solicitud);
        return new EmisionIdempotente(idempotencia, repositorio, new ComprobanteResponseMapperImpl(), huella,
            obligatoria, RETENCION);
    }

    /** Una emisión que "crea" un comprobante nuevo cada vez que corre y cuenta cuántas veces corrió. */
    private java.util.function.Supplier<Uni<ComprobanteResponse>> emisionQueCrea() {
        return () -> {
            emisiones.incrementAndGet();
            Comprobante creado = new Comprobante();
            creado.setId(UUID.randomUUID());
            comprobantes.put(creado.getId(), creado);
            ComprobanteResponse respuesta = new ComprobanteResponse();
            respuesta.setId(creado.getId());
            return Uni.createFrom().item(respuesta);
        };
    }

    private ResultadoEmision ejecutar(EmisionIdempotente servicio, String clave, Object solicitud) {
        return servicio.ejecutar(RUC, clave, TipoComprobante.FACTURA, solicitud, emisionQueCrea())
            .await().indefinitely();
    }

    @Test
    void testDeberiaLaPrimeraSolicitudEmiteYGuardaElComprobanteConLaClave() {
        ResultadoEmision resultado = ejecutar(crearServicio(false), CLAVE, "cuerpo-A");

        assertFalse(resultado.repetida());
        assertEquals(1, emisiones.get());
        assertEquals(resultado.comprobante().getId(), idempotencia.filas.get(RUC + CLAVE).comprobanteId());
    }

    @Test
    void testDeberiaRepetirLaMismaClaveConElMismoCuerpoDevuelveElMismoComprobanteSinEmitirOtro() {
        EmisionIdempotente servicio = crearServicio(false);

        ResultadoEmision primera = ejecutar(servicio, CLAVE, "cuerpo-A");
        ResultadoEmision segunda = ejecutar(servicio, CLAVE, "cuerpo-A");

        assertTrue(segunda.repetida());
        assertEquals(1, emisiones.get());
        assertEquals(primera.comprobante().getId(), segunda.comprobante().getId());
    }

    @Test
    void testDeberiaLaMismaClaveConOtroCuerpoEsUnErrorDelCliente() {
        EmisionIdempotente servicio = crearServicio(false);
        ejecutar(servicio, CLAVE, "cuerpo-A");

        ApiException e = assertThrows(ApiException.class, () -> ejecutar(servicio, CLAVE, "cuerpo-B"));

        assertEquals(CodigoError.IDEMPOTENCIA_CLAVE_REUTILIZADA, e.getError());
        assertEquals(1, emisiones.get());
    }

    @Test
    void testDeberiaLaMismaClaveConOtroTipoDeComprobanteTampocoSeReutiliza() {
        EmisionIdempotente servicio = crearServicio(false);
        ejecutar(servicio, CLAVE, "cuerpo-A");

        ApiException e = assertThrows(ApiException.class, () -> servicio
            .ejecutar(RUC, CLAVE, TipoComprobante.NOTA_CREDITO, "cuerpo-A", emisionQueCrea()).await().indefinitely());

        assertEquals(CodigoError.IDEMPOTENCIA_CLAVE_REUTILIZADA, e.getError());
    }

    @Test
    void testDeberiaSiLaSolicitudOriginalSigueEnCursoElReintentoRecibeUnConflicto() {
        EmisionIdempotente servicio = crearServicio(false);
        // Otra transacción reservó la clave y todavía no termina: la fila existe pero sin comprobante.
        servicio.ejecutar(RUC, CLAVE, TipoComprobante.FACTURA, "cuerpo-A", () -> Uni.createFrom().nothing())
            .subscribe().with(ignorado -> { }, ignorado -> { });

        ApiException e = assertThrows(ApiException.class, () -> ejecutar(servicio, CLAVE, "cuerpo-A"));

        assertEquals(CodigoError.IDEMPOTENCIA_EN_CURSO, e.getError());
    }

    @Test
    void testDeberiaSinClaveSeEmiteSiempreSalvoQueSeaObligatoria() {
        EmisionIdempotente opcional = crearServicio(false);
        ejecutar(opcional, null, "cuerpo-A");
        ejecutar(opcional, "  ", "cuerpo-A");
        assertEquals(2, emisiones.get());
        assertTrue(idempotencia.filas.isEmpty());

        ApiException e = assertThrows(ApiException.class, () -> ejecutar(crearServicio(true), null, "cuerpo-A"));
        assertEquals(CodigoError.IDEMPOTENCIA_CLAVE_REQUERIDA, e.getError());
        assertEquals(2, emisiones.get());
    }

    @Test
    void testDeberiaUnaClaveConFormatoInvalidoSeRechazaSinEmitir() {
        EmisionIdempotente servicio = crearServicio(false);
        for (String invalida : new String[] {"corta", "con espacios y mas", "x".repeat(EmisionIdempotente.LONGITUD_MAXIMA + 1)}) {
            ApiException e = assertThrows(ApiException.class, () -> ejecutar(servicio, invalida, "cuerpo-A"));
            assertEquals(CodigoError.IDEMPOTENCIA_CLAVE_INVALIDA, e.getError());
        }
        assertEquals(0, emisiones.get());
    }

    @Test
    void testDeberiaSiLaEmisionFallaLaClaveNoQuedaAsociadaYElReintentoPuedeVolverAEmitir() {
        EmisionIdempotente servicio = crearServicio(false);
        assertThrows(IllegalStateException.class, () -> servicio.ejecutar(RUC, CLAVE, TipoComprobante.FACTURA,
            "cuerpo-A", () -> Uni.createFrom().failure(new IllegalStateException("falla"))).await().indefinitely());

        // En producción la transacción se deshace y con ella la reserva; aquí se simula borrándola.
        idempotencia.filas.clear();
        ResultadoEmision resultado = ejecutar(servicio, CLAVE, "cuerpo-A");

        assertFalse(resultado.repetida());
        assertEquals(1, emisiones.get());
    }

    @Test
    void testDeberiaLaPurgaBorraLasClavesMasViejasQueLaRetencion() {
        long borradas = crearServicio(false).purgarVencidas().await().indefinitely();

        assertEquals(3L, borradas);
        assertNotEquals(null, idempotencia.limitePurga);
        assertTrue(Duration.between(idempotencia.limitePurga, Instant.now()).compareTo(RETENCION) >= 0);
    }
}
