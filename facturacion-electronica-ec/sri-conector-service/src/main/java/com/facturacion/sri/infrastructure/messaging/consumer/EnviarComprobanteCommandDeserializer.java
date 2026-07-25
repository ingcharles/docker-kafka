package com.facturacion.sri.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

import com.facturacion.sri.application.event.EnviarComprobanteCommand;

public class EnviarComprobanteCommandDeserializer extends ObjectMapperDeserializer<EnviarComprobanteCommand> {

    public EnviarComprobanteCommandDeserializer() {
        super(EnviarComprobanteCommand.class);
    }
}
