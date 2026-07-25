package com.facturacion.comprobantes.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

import com.facturacion.comprobantes.application.event.EmisorEventMessage;

public class EmisorEventMessageDeserializer extends ObjectMapperDeserializer<EmisorEventMessage> {

    public EmisorEventMessageDeserializer() {
        super(EmisorEventMessage.class);
    }
}
