package com.facturacion.inventario.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

import com.facturacion.inventario.application.event.StockCommandMessage;

public class StockCommandMessageDeserializer extends ObjectMapperDeserializer<StockCommandMessage> {

    public StockCommandMessageDeserializer() {
        super(StockCommandMessage.class);
    }
}
