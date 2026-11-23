package com.facturacion.identidad.infrastructure.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.facturacion.commons.exception.ErrorNegocioException;
import com.facturacion.identidad.shared.error.IdentidadError;

/** El hash y la verificación salen por un Uni, y una ráfaga no encola sin límite: responde 429. */
class BcryptHashDePasswordTest {

    private static final Duration ESPERA = Duration.ofSeconds(10);

    private final BcryptHashDePassword hasher = new BcryptHashDePassword(2, 10);

    @Test
    void testDeberiaGenerarUnHashQueVerificaLaContrasenaYRechazaOtra() {
        String hash = hasher.generar("secreta-123").await().atMost(ESPERA);

        assertNotEquals("secreta-123", hash);
        assertTrue(hasher.verificar("secreta-123", hash).await().atMost(ESPERA));
        assertFalse(hasher.verificar("otra", hash).await().atMost(ESPERA));
    }

    @Test
    void testDeberiaNoCoincidirNuncaSinCuentaAunqueLaContrasenaSeaLaDescartable() {
        assertFalse(hasher.verificar("contraseña-descartable-sin-cuenta", null).await().atMost(ESPERA));
        assertFalse(hasher.verificar("lo-que-sea", null).await().atMost(ESPERA));
    }

    @Test
    void testDeberiaResponderUn429EnVezDeEncolarSinLimiteConTodosLosHilosOcupadosYLaColaLlena() throws Exception {
        BcryptHashDePassword pequeno = new BcryptHashDePassword(1, 1);
        List<CompletableFuture<String>> pedidos = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            pedidos.add(pequeno.generar("clave-" + i).subscribeAsCompletionStage());
        }

        int rechazados = 0;
        for (CompletableFuture<String> pedido : pedidos) {
            try {
                pedido.get(15, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                assertTrue(e.getCause() instanceof ErrorNegocioException, e.getCause().toString());
                assertEquals(IdentidadError.AUTENTICACION_SATURADA, ((ErrorNegocioException) e.getCause()).getError());
                assertEquals(429, ((ErrorNegocioException) e.getCause()).getError().estadoHttp());
                rechazados++;
            }
        }
        assertTrue(rechazados >= 1, "con 1 hilo y cola de 1, al menos una de 6 peticiones simultáneas debe rechazarse");
        pequeno.cerrar();
    }
}
