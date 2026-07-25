package com.facturacion.orquestacion.infrastructure.messaging.consumer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

import com.facturacion.orquestacion.application.event.ComprobanteEventMessage;

/**
 * {@code facturacion.comprobante.eventos.v1} lleva más de un tipo de
 * mensaje ({@code ComprobanteCreado} y {@code ComprobanteAutorizado}) — sin
 * {@code FAIL_ON_UNKNOWN_PROPERTIES=false} cualquier {@code ComprobanteAutorizado}
 * (que trae campos que este record no tiene) rompería la deserialización y
 * terminaría en la dead-letter-queue en vez de simplemente ignorarse.
 */
public class ComprobanteEventMessageDeserializer extends ObjectMapperDeserializer<ComprobanteEventMessage> {

    public ComprobanteEventMessageDeserializer() {
        super(ComprobanteEventMessage.class, construirObjectMapper());
    }

    private static ObjectMapper construirObjectMapper() {
        return new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
