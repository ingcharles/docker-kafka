package com.facturacion.orquestacion.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

import com.facturacion.orquestacion.application.event.SriResultadoEventMessage;

public class SriResultadoEventMessageDeserializer extends ObjectMapperDeserializer<SriResultadoEventMessage> {

    public SriResultadoEventMessageDeserializer() {
        super(SriResultadoEventMessage.class);
    }
}
