package com.facturacion.sri.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;

import com.facturacion.sri.application.event.EnviarComprobanteCommand;

/** La estrategia dead-letter-queue del canal {@code sri-comandos-in} la necesita por convención de nombres. */
public class EnviarComprobanteCommandSerializer extends ObjectMapperSerializer<EnviarComprobanteCommand> {
}
