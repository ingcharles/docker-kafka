package com.facturacion.identidad.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;

import com.facturacion.identidad.application.event.EmpresaEventMessage;

/** La estrategia dead-letter-queue del canal {@code empresa-events-in} la necesita por convención de nombres. */
public class EmpresaEventMessageSerializer extends ObjectMapperSerializer<EmpresaEventMessage> {
}
