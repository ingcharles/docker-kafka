package com.facturacion.firma.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

import com.facturacion.firma.application.event.FirmarComprobanteCommand;

public class FirmarComprobanteCommandDeserializer extends ObjectMapperDeserializer<FirmarComprobanteCommand> {

    public FirmarComprobanteCommandDeserializer() {
        super(FirmarComprobanteCommand.class);
    }
}
