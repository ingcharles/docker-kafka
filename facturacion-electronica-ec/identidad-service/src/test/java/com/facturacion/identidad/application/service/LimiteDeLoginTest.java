package com.facturacion.identidad.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;

import com.facturacion.commons.exception.LimiteExcedidoException;
import com.facturacion.commons.limite.ContadorDeUso;
import com.facturacion.commons.limite.Limite;
import com.facturacion.commons.limite.LimitadorDeUso;
import com.facturacion.commons.limite.Uso;

class LimiteDeLoginTest {

    private static final int MAXIMO = 3;
    private static final String CORREO = "ana@x.com";

    private final Map<String, Integer> usos = new HashMap<>();
    private final List<String> claves = new ArrayList<>();
    private int verificaciones;

    private final LimiteDeLogin limite = new LimiteDeLogin(new LimitadorDeUso(new ContadorDeUso() {
        @Override
        public Uni<Uso> consumir(String clave, Duration ventana) {
            claves.add(clave);
            return Uni.createFrom().item(new Uso(usos.merge(clave, 1, Integer::sum), 60));
        }

        @Override
        public Uni<Void> reiniciar(String clave) {
            usos.remove(clave);
            return Uni.createFrom().voidItem();
        }
    }, (operacion, ruc) -> Uni.createFrom().item(Optional.of(new Limite(MAXIMO, 0, Duration.ofMinutes(15))))));

    private String intentar(String correo, boolean claveCorrecta) {
        Supplier<Uni<String>> iniciarSesion = () -> {
            verificaciones++;
            return claveCorrecta ? Uni.createFrom().item("token")
                : Uni.createFrom().failure(new IllegalStateException("credenciales"));
        };
        return limite.proteger(correo, iniciarSesion).await().indefinitely();
    }

    private void fallar(String correo, int veces) {
        for (int i = 0; i < veces; i++) {
            assertThrows(IllegalStateException.class, () -> intentar(correo, false));
        }
    }

    @Test
    void testDeberiaRechazarSinVerificarLaClaveAlAgotarLosIntentos() {
        fallar(CORREO, MAXIMO);

        assertThrows(LimiteExcedidoException.class, () -> intentar(CORREO, true));
        assertEquals(MAXIMO, verificaciones);
    }

    @Test
    void testDeberiaVolverACeroCuandoElInicioDeSesionEntra() {
        fallar(CORREO, MAXIMO - 1);
        intentar(CORREO, true);

        fallar(CORREO, MAXIMO);
    }

    @Test
    void testDeberiaContarComoLaMismaCuentaElCorreoConMayusculasOEspacios() {
        fallar(CORREO, MAXIMO);

        assertThrows(LimiteExcedidoException.class, () -> intentar("  ANA@X.com ", true));
    }

    @Test
    void testDeberiaGuardarSoloLaHuellaDelCorreo() {
        fallar(CORREO, 1);

        assertFalse(claves.get(0).contains(CORREO));
    }
}
