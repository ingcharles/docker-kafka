package com.facturacion.identidad.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

import com.facturacion.identidad.application.event.EstablecimientoPuntoEmisionEventMessage;

public class EstablecimientoPuntoEmisionEventMessageDeserializer
        extends ObjectMapperDeserializer<EstablecimientoPuntoEmisionEventMessage> {

    public EstablecimientoPuntoEmisionEventMessageDeserializer() {
        super(EstablecimientoPuntoEmisionEventMessage.class);
    }
}
