package com.ecommerce.saga.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

import com.ecommerce.saga.application.event.PaymentEventMessage;

public class PaymentEventMessageDeserializer extends ObjectMapperDeserializer<PaymentEventMessage> {

    public PaymentEventMessageDeserializer() {
        super(PaymentEventMessage.class);
    }
}
