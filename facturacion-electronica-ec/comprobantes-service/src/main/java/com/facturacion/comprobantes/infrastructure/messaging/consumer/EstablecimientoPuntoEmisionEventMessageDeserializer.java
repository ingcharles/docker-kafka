package com.facturacion.comprobantes.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

import com.facturacion.comprobantes.application.event.EstablecimientoPuntoEmisionEventMessage;

public class EstablecimientoPuntoEmisionEventMessageDeserializer
        extends ObjectMapperDeserializer<EstablecimientoPuntoEmisionEventMessage> {

    public EstablecimientoPuntoEmisionEventMessageDeserializer() {
        super(EstablecimientoPuntoEmisionEventMessage.class);
    }
}
