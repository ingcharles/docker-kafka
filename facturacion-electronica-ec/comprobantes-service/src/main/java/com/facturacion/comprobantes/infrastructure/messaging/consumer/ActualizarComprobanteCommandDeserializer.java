package com.facturacion.comprobantes.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

import com.facturacion.comprobantes.application.event.ActualizarComprobanteCommand;

public class ActualizarComprobanteCommandDeserializer extends ObjectMapperDeserializer<ActualizarComprobanteCommand> {

    public ActualizarComprobanteCommandDeserializer() {
        super(ActualizarComprobanteCommand.class);
    }
}
