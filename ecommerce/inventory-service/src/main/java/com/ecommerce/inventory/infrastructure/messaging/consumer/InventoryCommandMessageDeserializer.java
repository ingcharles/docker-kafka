package com.ecommerce.inventory.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

import com.ecommerce.inventory.application.event.InventoryCommandMessage;

public class InventoryCommandMessageDeserializer extends ObjectMapperDeserializer<InventoryCommandMessage> {

    public InventoryCommandMessageDeserializer() {
        super(InventoryCommandMessage.class);
    }
}
