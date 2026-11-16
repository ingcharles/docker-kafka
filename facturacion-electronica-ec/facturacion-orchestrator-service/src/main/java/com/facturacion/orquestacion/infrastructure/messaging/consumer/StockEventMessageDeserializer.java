package com.facturacion.orquestacion.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

import com.facturacion.orquestacion.application.event.StockEventMessage;

public class StockEventMessageDeserializer extends ObjectMapperDeserializer<StockEventMessage> {

    public StockEventMessageDeserializer() {
        super(StockEventMessage.class);
    }
}
