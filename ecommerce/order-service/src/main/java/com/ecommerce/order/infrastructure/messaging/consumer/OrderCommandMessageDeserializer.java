package com.ecommerce.order.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

import com.ecommerce.order.application.event.OrderCommandMessage;

public class OrderCommandMessageDeserializer extends ObjectMapperDeserializer<OrderCommandMessage> {

    public OrderCommandMessageDeserializer() {
        super(OrderCommandMessage.class);
    }
}
