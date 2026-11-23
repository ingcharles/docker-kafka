package com.facturacion.identidad.infrastructure.messaging.producer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.TimeoutException;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * El ack de Kafka llega en un hilo del productor; la revocacion debe seguir en el hilo del contexto de Vert.x donde se
 * abrio la transaccion (si no, Hibernate Reactive falla con HR000069 y el DELETE responde 500).
 */
class KafkaRevocacionApiKeysPublicadorTest {

    private static final Duration ESPERA = Duration.ofSeconds(5);

    /** Confirma el mensaje desde un hilo ajeno, como el hilo de red del productor de Kafka. */
    private static final class EmisorQueConfirmaEnOtroHilo implements Emitter<String> {
        @Override
        public <M extends Message<? extends String>> void send(M mensaje) {
            // El retardo imita la ida y vuelta al broker: el ack llega despues de que el Uni ya esta suscrito.
            Thread productor = new Thread(() -> {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                mensaje.ack();
            }, "kafka-producer-network-thread-falso");
            productor.start();
        }

        @Override
        public CompletionStage<Void> send(String carga) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void complete() {
        }

        @Override
        public void error(Exception e) {
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean hasRequests() {
            return true;
        }
    }

    @Test
    void testDeberiaContinuaEnElHiloDelContextoDeVertxDespuesDelAckDeKafka() throws Exception {
        Vertx vertx = Vertx.vertx();
        try {
            KafkaRevocacionApiKeysPublicador publicador =
                new KafkaRevocacionApiKeysPublicador(new EmisorQueConfirmaEnOtroHilo(), ESPERA);
            CompletableFuture<String> hiloDelContexto = new CompletableFuture<>();
            CompletableFuture<String> hiloDeLaContinuacion = new CompletableFuture<>();

            Context contexto = vertx.getOrCreateContext();
            contexto.runOnContext(ignorado -> {
                hiloDelContexto.complete(Thread.currentThread().getName());
                publicador.publicar("jti-1", Instant.now().plusSeconds(60))
                    .subscribe().with(
                        v -> hiloDeLaContinuacion.complete(Thread.currentThread().getName()),
                        hiloDeLaContinuacion::completeExceptionally);
            });

            assertEquals(hiloDelContexto.get(5, TimeUnit.SECONDS), hiloDeLaContinuacion.get(5, TimeUnit.SECONDS));
            assertTrue(hiloDelContexto.get().startsWith("vert.x-eventloop-thread"));
        } finally {
            vertx.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void testDeberiaSinContextoDeVertxTambienCompletaTrasElAck() {
        KafkaRevocacionApiKeysPublicador publicador =
            new KafkaRevocacionApiKeysPublicador(new EmisorQueConfirmaEnOtroHilo(), ESPERA);
        AtomicReference<String> hilo = new AtomicReference<>();

        publicador.publicar("jti-2", Instant.now().plusSeconds(60))
            .invoke(() -> hilo.set(Thread.currentThread().getName()))
            .await().atMost(Duration.ofSeconds(5));

        assertNotEquals(null, hilo.get());
    }
    /** Un broker caído: el mensaje nunca se confirma. */
    private static final class EmisorSinRespuesta implements Emitter<String> {
        @Override
        public <M extends Message<? extends String>> void send(M mensaje) {
        }

        @Override
        public CompletionStage<Void> send(String carga) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void complete() {
        }

        @Override
        public void error(Exception e) {
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean hasRequests() {
            return true;
        }
    }

    @Test
    void testDeberiaSinAckDelBrokerLaRevocacionFallaAlVencerElPlazoEnVezDeColgarse() {
        KafkaRevocacionApiKeysPublicador publicador =
            new KafkaRevocacionApiKeysPublicador(new EmisorSinRespuesta(), Duration.ofMillis(200));

        CompletionException e = assertThrows(CompletionException.class, () -> publicador
            .publicar("jti-3", Instant.now().plusSeconds(60)).subscribeAsCompletionStage().join());

        assertTrue(e.getCause() instanceof TimeoutException, e.getCause().toString());
    }
}
