package com.ecommerce.saga.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

import com.ecommerce.saga.application.event.NotificationEventMessage;

public class NotificationEventMessageDeserializer extends ObjectMapperDeserializer<NotificationEventMessage> {

    public NotificationEventMessageDeserializer() {
        super(NotificationEventMessage.class);
    }
}
