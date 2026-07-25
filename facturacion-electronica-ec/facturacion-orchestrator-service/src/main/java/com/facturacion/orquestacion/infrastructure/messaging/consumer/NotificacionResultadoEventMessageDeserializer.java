package com.facturacion.orquestacion.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

import com.facturacion.orquestacion.application.event.NotificacionResultadoEventMessage;

public class NotificacionResultadoEventMessageDeserializer
        extends ObjectMapperDeserializer<NotificacionResultadoEventMessage> {

    public NotificacionResultadoEventMessageDeserializer() {
        super(NotificacionResultadoEventMessage.class);
    }
}
