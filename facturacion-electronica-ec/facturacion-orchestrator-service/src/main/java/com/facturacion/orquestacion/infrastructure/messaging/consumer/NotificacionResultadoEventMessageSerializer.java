package com.facturacion.orquestacion.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;

import com.facturacion.orquestacion.application.event.NotificacionResultadoEventMessage;

/** La estrategia dead-letter-queue del canal {@code notificacion-events-in} la necesita por convención de nombres. */
public class NotificacionResultadoEventMessageSerializer
        extends ObjectMapperSerializer<NotificacionResultadoEventMessage> {
}
