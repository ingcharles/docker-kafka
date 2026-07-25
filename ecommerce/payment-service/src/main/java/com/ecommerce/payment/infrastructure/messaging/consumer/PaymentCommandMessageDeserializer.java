package com.ecommerce.payment.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

import com.ecommerce.payment.application.event.PaymentCommandMessage;

public class PaymentCommandMessageDeserializer extends ObjectMapperDeserializer<PaymentCommandMessage> {

    public PaymentCommandMessageDeserializer() {
        super(PaymentCommandMessage.class);
    }
}
