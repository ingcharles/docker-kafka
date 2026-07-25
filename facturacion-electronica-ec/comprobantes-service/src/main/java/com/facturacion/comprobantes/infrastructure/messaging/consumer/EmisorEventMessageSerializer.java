package com.facturacion.comprobantes.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;

import com.facturacion.comprobantes.application.event.EmisorEventMessage;

/** La estrategia dead-letter-queue del canal {@code emisor-events-in} la necesita por convención de nombres. */
public class EmisorEventMessageSerializer extends ObjectMapperSerializer<EmisorEventMessage> {
}
