package com.ecommerce.shipping.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

import com.ecommerce.shipping.application.event.ShippingCommandMessage;

public class ShippingCommandMessageDeserializer extends ObjectMapperDeserializer<ShippingCommandMessage> {

    public ShippingCommandMessageDeserializer() {
        super(ShippingCommandMessage.class);
    }
}
