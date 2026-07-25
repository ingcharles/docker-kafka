package com.facturacion.comprobantes.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

import com.facturacion.comprobantes.application.event.ItemEventMessage;

public class ItemEventMessageDeserializer extends ObjectMapperDeserializer<ItemEventMessage> {

    public ItemEventMessageDeserializer() {
        super(ItemEventMessage.class);
    }
}
