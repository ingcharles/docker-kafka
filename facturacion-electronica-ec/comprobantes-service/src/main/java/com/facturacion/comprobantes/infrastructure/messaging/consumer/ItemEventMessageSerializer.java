package com.facturacion.comprobantes.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;

import com.facturacion.comprobantes.application.event.ItemEventMessage;

/** La estrategia dead-letter-queue del canal {@code item-events-in} la necesita por convención de nombres. */
public class ItemEventMessageSerializer extends ObjectMapperSerializer<ItemEventMessage> {
}
