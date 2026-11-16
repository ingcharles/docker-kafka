package com.facturacion.orquestacion.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;

/** La estrategia dead-letter-queue del canal {@code inventario-events-in} la necesita por convención de nombres. */
public class StockEventMessageSerializer extends ObjectMapperSerializer<com.facturacion.orquestacion.application.event.StockEventMessage> {
}
