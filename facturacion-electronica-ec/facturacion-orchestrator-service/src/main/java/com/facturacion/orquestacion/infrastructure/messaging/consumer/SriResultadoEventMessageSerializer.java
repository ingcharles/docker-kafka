package com.facturacion.orquestacion.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;

import com.facturacion.orquestacion.application.event.SriResultadoEventMessage;

/** La estrategia dead-letter-queue del canal {@code sri-events-in} la necesita por convención de nombres. */
public class SriResultadoEventMessageSerializer extends ObjectMapperSerializer<SriResultadoEventMessage> {
}
