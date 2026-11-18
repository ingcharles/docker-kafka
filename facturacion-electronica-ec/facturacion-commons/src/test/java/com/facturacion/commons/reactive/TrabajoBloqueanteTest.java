package com.facturacion.commons.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/** El trabajo bloqueante corre fuera del hilo del llamador y el resultado vuelve a su contexto de Vert.x. */
class TrabajoBloqueanteTest {

    private static final Duration ESPERA = Duration.ofSeconds(5);

    @Test
    void sinContextoDeVertxDevuelveElResultadoDelTrabajo() {
        assertEquals("listo", TrabajoBloqueante.ejecutar(() -> "listo").await().atMost(ESPERA));
    }

    @Test
    void conContextoElTrabajoCorreEnOtroHiloYElResultadoVuelveAlContextoDelLlamador() throws Exception {
        Vertx vertx = Vertx.vertx();
        try {
            CompletableFuture<Object[]> resultado = new CompletableFuture<>();
            vertx.getOrCreateContext().runOnContext(ignorado -> {
                Context contextoDelLlamador = Vertx.currentContext();
                String hiloDelLlamador = Thread.currentThread().getName();
                TrabajoBloqueante.ejecutar(() -> Thread.currentThread().getName())
                    .subscribe().with(hiloDelTrabajo -> resultado.complete(new Object[] {
                        hiloDelLlamador, hiloDelTrabajo, Vertx.currentContext() == contextoDelLlamador}),
                        resultado::completeExceptionally);
            });
            Object[] datos = resultado.get(5, TimeUnit.SECONDS);

            assertNotEquals(datos[0], datos[1], "el trabajo no debe correr en el hilo del llamador");
            assertTrue((Boolean) datos[2], "el resultado debe volver al contexto del llamador");
        } finally {
            vertx.close();
        }
    }

    @Test
    void conUnEjecutorPropioLlenoFallaEnVezDeEncolarSinLimite() throws Exception {
        ThreadPoolExecutor ejecutor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1));
        CountDownLatch liberar = new CountDownLatch(1);
        try {
            Uni<String> ocupado = TrabajoBloqueante.ejecutar(() -> espera(liberar), ejecutor);
            Uni<String> enCola = TrabajoBloqueante.ejecutar(() -> espera(liberar), ejecutor);
            CompletableFuture<String> a = ocupado.subscribeAsCompletionStage();
            CompletableFuture<String> b = enCola.subscribeAsCompletionStage();

            Exception rechazada = assertThrows(Exception.class, () -> TrabajoBloqueante
                .ejecutar(() -> "sobra", ejecutor).await().atMost(ESPERA));
            assertTrue(rechazada instanceof RejectedExecutionException
                || rechazada.getCause() instanceof RejectedExecutionException, rechazada.toString());

            liberar.countDown();
            assertEquals("fin", a.get(5, TimeUnit.SECONDS));
            assertEquals("fin", b.get(5, TimeUnit.SECONDS));
        } finally {
            liberar.countDown();
            ejecutor.shutdownNow();
        }
    }

    private static String espera(CountDownLatch liberar) {
        try {
            liberar.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "fin";
    }
}
