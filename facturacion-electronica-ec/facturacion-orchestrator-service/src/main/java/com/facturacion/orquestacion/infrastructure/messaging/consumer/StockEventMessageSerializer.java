package com.facturacion.orquestacion.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;

import com.facturacion.orquestacion.application.event.StockEventMessage;

/** La estrategia dead-letter-queue del canal {@code inventario-events-in} la necesita por convención de nombres. */
public class StockEventMessageSerializer extends ObjectMapperSerializer<StockEventMessage> {
}
