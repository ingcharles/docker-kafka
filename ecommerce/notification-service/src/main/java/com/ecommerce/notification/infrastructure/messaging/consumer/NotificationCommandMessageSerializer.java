package com.ecommerce.notification.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;

import com.ecommerce.notification.application.event.NotificationCommandMessage;

public class NotificationCommandMessageSerializer
    extends ObjectMapperSerializer<NotificationCommandMessage> {
}
