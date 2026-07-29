package com.facturacion.identidad.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

import com.facturacion.identidad.application.event.EmpresaEventMessage;

public class EmpresaEventMessageDeserializer extends ObjectMapperDeserializer<EmpresaEventMessage> {

    public EmpresaEventMessageDeserializer() {
        super(EmpresaEventMessage.class);
    }
}
