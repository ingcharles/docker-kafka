package com.facturacion.inventario.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;

import com.facturacion.inventario.application.event.StockCommandMessage;

/**
 * La estrategia dead-letter-queue del canal {@code inventario-comandos-in}
 * la necesita por convención de nombres a partir de
 * {@link StockCommandMessageDeserializer}.
 */
public class StockCommandMessageSerializer extends ObjectMapperSerializer<StockCommandMessage> {
}
