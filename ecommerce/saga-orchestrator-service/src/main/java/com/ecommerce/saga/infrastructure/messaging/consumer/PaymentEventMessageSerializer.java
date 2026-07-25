package com.ecommerce.saga.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;

import com.ecommerce.saga.application.event.PaymentEventMessage;

public class PaymentEventMessageSerializer extends ObjectMapperSerializer<PaymentEventMessage> {
}
