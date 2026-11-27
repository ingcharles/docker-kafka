package com.facturacion.commons.limite;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;

import com.facturacion.commons.exception.LimiteExcedidoException;

class LimitadorDeUsoTest {

    private static final Duration VENTANA = Duration.ofMinutes(1);
    private static final long SEGUNDOS_RESTANTES = 42;
    private static final String USUARIO = "usuario-1";
    private static final String OTRO_USUARIO = "usuario-2";
    private static final String EMPRESA = "1790011001001";
    private static final String OTRA_EMPRESA = "1790022002001";

    /** Contador en memoria sin caducidad: suficiente para la lógica de quien limita. */
    private static final class ContadorEnMemoria implements ContadorDeUso {

        private final Map<String, Integer> usos = new HashMap<>();

        @Override
        public Uni<Uso> consumir(String clave, Duration ventana) {
            return Uni.createFrom().item(() -> new Uso(usos.merge(clave, 1, Integer::sum), SEGUNDOS_RESTANTES));
        }

        @Override
        public Uni<Void> reiniciar(String clave) {
            usos.remove(clave);
            return Uni.createFrom().voidItem();
        }
    }

    /** Contador caído: toda llamada falla. */
    private static final class ContadorCaido implements ContadorDeUso {

        @Override
        public Uni<Uso> consumir(String clave, Duration ventana) {
            return Uni.createFrom().failure(new IllegalStateException("sin base de datos"));
        }

        @Override
        public Uni<Void> reiniciar(String clave) {
            return Uni.createFrom().failure(new IllegalStateException("sin base de datos"));
        }
    }

    private static ResolutorDeLimites fijar(int porUsuario, int porEmpresa) {
        return (operacion, ruc) -> Uni.createFrom().item(Optional.of(new Limite(porUsuario, porEmpresa, VENTANA)));
    }

    private static LimitadorDeUso crearLimitador(int porUsuario, int porEmpresa) {
        return new LimitadorDeUso(new ContadorEnMemoria(), fijar(porUsuario, porEmpresa));
    }

    private static void exigir(LimitadorDeUso limitador, String usuario, String ruc) {
        limitador.exigir(OperacionLimitada.EMISION, usuario, ruc).await().indefinitely();
    }

    @Test
    void testDeberiaRechazarSoloLoQueSuperaElMaximoEIndicarCuantoEsperar() {
        LimitadorDeUso limitador = crearLimitador(2, 0);
        exigir(limitador, USUARIO, EMPRESA);
        exigir(limitador, USUARIO, EMPRESA);

        LimiteExcedidoException rechazo = assertThrows(LimiteExcedidoException.class,
            () -> exigir(limitador, USUARIO, EMPRESA));

        assertEquals(SEGUNDOS_RESTANTES, rechazo.getSegundosEspera());
        assertEquals(429, rechazo.getError().estadoHttp());
    }

    @Test
    void testDeberiaContarPorSeparadoCadaUsuarioCadaEmpresaYCadaOperacion() {
        LimitadorDeUso limitador = crearLimitador(1, 0);
        exigir(limitador, USUARIO, EMPRESA);

        assertDoesNotThrow(() -> exigir(limitador, OTRO_USUARIO, EMPRESA));
        assertDoesNotThrow(() -> exigir(limitador, USUARIO, OTRA_EMPRESA));
        assertDoesNotThrow(() -> limitador.exigir(OperacionLimitada.REENVIO_CORREO, USUARIO, EMPRESA)
            .await().indefinitely());
    }

    @Test
    void testDeberiaSumarATodosLosUsuariosEnElLimiteDeLaEmpresa() {
        LimitadorDeUso limitador = crearLimitador(0, 2);
        exigir(limitador, USUARIO, EMPRESA);
        exigir(limitador, OTRO_USUARIO, EMPRESA);

        assertThrows(LimiteExcedidoException.class, () -> exigir(limitador, "usuario-3", EMPRESA));
        assertDoesNotThrow(() -> exigir(limitador, USUARIO, OTRA_EMPRESA));
    }

    @Test
    void testDeberiaNoGastarElCupoDeLaEmpresaConQuienYaSuperoElSuyo() {
        LimitadorDeUso limitador = crearLimitador(1, 2);
        exigir(limitador, USUARIO, EMPRESA);
        assertThrows(LimiteExcedidoException.class, () -> exigir(limitador, USUARIO, EMPRESA));

        assertDoesNotThrow(() -> exigir(limitador, OTRO_USUARIO, EMPRESA));
    }

    @Test
    void testDeberiaAplicarACadaEmpresaSuPropioLimite() {
        LimitadorDeUso limitador = new LimitadorDeUso(new ContadorEnMemoria(), (operacion, ruc) -> Uni.createFrom()
            .item(Optional.of(new Limite(EMPRESA.equals(ruc) ? 1 : 2, 0, VENTANA))));
        exigir(limitador, USUARIO, EMPRESA);
        exigir(limitador, USUARIO, OTRA_EMPRESA);

        assertThrows(LimiteExcedidoException.class, () -> exigir(limitador, USUARIO, EMPRESA));
        assertDoesNotThrow(() -> exigir(limitador, USUARIO, OTRA_EMPRESA));
    }

    @Test
    void testDeberiaDejarPasarSinLimiteConfigurado() {
        LimitadorDeUso limitador = new LimitadorDeUso(new ContadorCaido(),
            (operacion, ruc) -> Uni.createFrom().item(Optional.empty()));

        assertDoesNotThrow(() -> exigir(limitador, USUARIO, EMPRESA));
    }

    @Test
    void testDeberiaDejarPasarCuandoLaBaseNoResponde() {
        LimitadorDeUso sinContador = new LimitadorDeUso(new ContadorCaido(), fijar(1, 1));
        LimitadorDeUso sinConfiguracion = new LimitadorDeUso(new ContadorEnMemoria(),
            (operacion, ruc) -> Uni.createFrom().failure(new IllegalStateException("sin base de datos")));

        assertDoesNotThrow(() -> exigir(sinContador, USUARIO, EMPRESA));
        assertDoesNotThrow(() -> exigir(sinConfiguracion, USUARIO, EMPRESA));
        assertDoesNotThrow(() -> sinContador.reiniciar(OperacionLimitada.EMISION, USUARIO, EMPRESA)
            .await().indefinitely());
    }

    @Test
    void testDeberiaVolverACeroAlReiniciar() {
        LimitadorDeUso limitador = crearLimitador(1, 0);
        exigir(limitador, USUARIO, null);

        limitador.reiniciar(OperacionLimitada.EMISION, USUARIO, null).await().indefinitely();

        assertDoesNotThrow(() -> exigir(limitador, USUARIO, null));
    }
}
