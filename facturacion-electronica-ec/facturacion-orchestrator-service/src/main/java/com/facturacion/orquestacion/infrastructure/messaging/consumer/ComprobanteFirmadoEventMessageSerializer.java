package com.facturacion.orquestacion.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;

import com.facturacion.orquestacion.application.event.ComprobanteFirmadoEventMessage;

/** La estrategia dead-letter-queue del canal {@code firma-events-in} la necesita por convención de nombres. */
public class ComprobanteFirmadoEventMessageSerializer extends ObjectMapperSerializer<ComprobanteFirmadoEventMessage> {
}
