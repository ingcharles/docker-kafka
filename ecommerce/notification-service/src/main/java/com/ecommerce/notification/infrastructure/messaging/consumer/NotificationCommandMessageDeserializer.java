package com.ecommerce.notification.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

import com.ecommerce.notification.application.event.NotificationCommandMessage;

public class NotificationCommandMessageDeserializer
    extends ObjectMapperDeserializer<NotificationCommandMessage> {

    public NotificationCommandMessageDeserializer() {
        super(NotificationCommandMessage.class);
    }
}
