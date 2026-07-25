package com.facturacion.orquestacion.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

import com.facturacion.orquestacion.application.event.ComprobanteFirmadoEventMessage;

public class ComprobanteFirmadoEventMessageDeserializer extends ObjectMapperDeserializer<ComprobanteFirmadoEventMessage> {

    public ComprobanteFirmadoEventMessageDeserializer() {
        super(ComprobanteFirmadoEventMessage.class);
    }
}
