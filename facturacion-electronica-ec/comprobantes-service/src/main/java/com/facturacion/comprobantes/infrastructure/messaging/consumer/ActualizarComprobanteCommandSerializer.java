package com.facturacion.comprobantes.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;

import com.facturacion.comprobantes.application.event.ActualizarComprobanteCommand;

/** La estrategia dead-letter-queue del canal {@code comprobante-comandos-in} la necesita por convención de nombres. */
public class ActualizarComprobanteCommandSerializer extends ObjectMapperSerializer<ActualizarComprobanteCommand> {
}
