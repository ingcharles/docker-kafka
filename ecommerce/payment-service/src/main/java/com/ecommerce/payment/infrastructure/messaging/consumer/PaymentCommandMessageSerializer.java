package com.ecommerce.payment.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;

import com.ecommerce.payment.application.event.PaymentCommandMessage;

public class PaymentCommandMessageSerializer extends ObjectMapperSerializer<PaymentCommandMessage> {
}
