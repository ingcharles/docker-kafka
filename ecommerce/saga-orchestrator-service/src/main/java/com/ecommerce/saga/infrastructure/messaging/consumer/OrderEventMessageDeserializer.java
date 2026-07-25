package com.ecommerce.saga.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

import com.ecommerce.saga.application.event.OrderEventMessage;

public class OrderEventMessageDeserializer extends ObjectMapperDeserializer<OrderEventMessage> {

    public OrderEventMessageDeserializer() {
        super(OrderEventMessage.class);
    }
}
