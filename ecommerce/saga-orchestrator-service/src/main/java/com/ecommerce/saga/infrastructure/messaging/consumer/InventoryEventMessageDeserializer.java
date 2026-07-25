package com.ecommerce.saga.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

import com.ecommerce.saga.application.event.InventoryEventMessage;

public class InventoryEventMessageDeserializer extends ObjectMapperDeserializer<InventoryEventMessage> {

    public InventoryEventMessageDeserializer() {
        super(InventoryEventMessage.class);
    }
}
