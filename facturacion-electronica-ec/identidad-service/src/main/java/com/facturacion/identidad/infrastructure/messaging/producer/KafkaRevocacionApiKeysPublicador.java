package com.facturacion.identidad.infrastructure.messaging.producer;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;

import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;

import com.facturacion.commons.security.MensajeRevocacion;
import com.facturacion.identidad.domain.port.RevocacionApiKeysPublicador;

/**
 * Publica en {@code facturacion.apikey.revocaciones.v1} con el jti como clave (tópico compactado: la última
 * revocación de cada key sobrevive aunque el consumidor arranque meses después). Espera la confirmación del broker y,
 * si no llega, falla: la transacción de la revocación se revierte y el usuario puede reintentar.
 */
@ApplicationScoped
public class KafkaRevocacionApiKeysPublicador implements RevocacionApiKeysPublicador {

    private static final Duration ESPERA_MAXIMA = Duration.ofSeconds(10);

    private final Emitter<String> emisor;

    @Inject
    public KafkaRevocacionApiKeysPublicador(@Channel("apikey-revocaciones-out") Emitter<String> emisor) {
        this.emisor = emisor;
    }

    @Override
    public Uni<Void> publicar(String jti, Instant venceEn) {
        // El ack llega en un hilo del productor de Kafka. La transaccion de la revocacion (Hibernate Reactive) debe
        // cerrarse en el contexto de Vert.x donde se abrio, asi que se vuelve a el despues de esperar la confirmacion.
        Context contexto = Vertx.currentContext();
        CompletableFuture<Void> confirmado = new CompletableFuture<>();
        Metadata metadata = Metadata.of(OutgoingKafkaRecordMetadata.<String>builder().withKey(jti).build());
        emisor.send(Message.of(new MensajeRevocacion(jti, venceEn).serializar(), metadata,
            () -> {
                confirmado.complete(null);
                return CompletableFuture.<Void>completedFuture(null);
            },
            causa -> {
                confirmado.completeExceptionally(causa);
                return CompletableFuture.<Void>completedFuture(null);
            }));
        Uni<Void> ack = Uni.createFrom().completionStage((CompletionStage<Void>) confirmado)
            .ifNoItem().after(ESPERA_MAXIMA).fail();
        return contexto == null ? ack : ack.emitOn(tarea -> contexto.runOnContext(ignorado -> tarea.run()));
    }
}
