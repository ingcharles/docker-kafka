package com.facturacion.identidad.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;

import com.facturacion.identidad.application.event.EmisorEventMessage;

/** La estrategia dead-letter-queue del canal {@code emisor-events-in} la necesita por convención de nombres. */
public class EmisorEventMessageSerializer extends ObjectMapperSerializer<EmisorEventMessage> {
}
