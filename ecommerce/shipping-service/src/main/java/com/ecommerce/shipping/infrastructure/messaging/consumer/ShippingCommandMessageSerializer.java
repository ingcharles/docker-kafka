package com.ecommerce.shipping.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;

import com.ecommerce.shipping.application.event.ShippingCommandMessage;

public class ShippingCommandMessageSerializer extends ObjectMapperSerializer<ShippingCommandMessage> {
}
