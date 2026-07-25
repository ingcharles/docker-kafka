package com.ecommerce.saga.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

import com.ecommerce.saga.application.event.ShippingEventMessage;

public class ShippingEventMessageDeserializer extends ObjectMapperDeserializer<ShippingEventMessage> {

    public ShippingEventMessageDeserializer() {
        super(ShippingEventMessage.class);
    }
}
