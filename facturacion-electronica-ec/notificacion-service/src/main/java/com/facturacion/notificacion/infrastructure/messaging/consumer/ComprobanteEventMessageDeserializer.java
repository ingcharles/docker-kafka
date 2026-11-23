package com.facturacion.notificacion.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.facturacion.notificacion.application.event.ComprobanteEventMessage;

/**
 * {@code facturacion.comprobante.eventos.v1} lleva más de un tipo de
 * mensaje (ver {@link ComprobanteEventMessage}) — sin
 * {@code FAIL_ON_UNKNOWN_PROPERTIES=false} cualquier {@code ComprobanteCreado}
 * (que trae {@code empresaRuc}/{@code xmlGenerado}, campos que este record no
 * tiene) rompería la deserialización y terminaría en la dead-letter-queue
 * por cada evento, no solo los que de verdad fallan.
 */
public class ComprobanteEventMessageDeserializer extends ObjectMapperDeserializer<ComprobanteEventMessage> {

    public ComprobanteEventMessageDeserializer() {
        super(ComprobanteEventMessage.class, construirObjectMapper());
    }

    private static ObjectMapper construirObjectMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
