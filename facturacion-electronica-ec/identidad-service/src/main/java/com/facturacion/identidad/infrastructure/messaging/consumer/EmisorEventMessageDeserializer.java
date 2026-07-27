package com.facturacion.identidad.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

import com.facturacion.identidad.application.event.EmisorEventMessage;

public class EmisorEventMessageDeserializer extends ObjectMapperDeserializer<EmisorEventMessage> {

    public EmisorEventMessageDeserializer() {
        super(EmisorEventMessage.class);
    }
}
