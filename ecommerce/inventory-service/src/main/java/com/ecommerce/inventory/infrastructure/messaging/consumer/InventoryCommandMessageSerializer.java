package com.ecommerce.inventory.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;

import com.ecommerce.inventory.application.event.InventoryCommandMessage;

/**
 * La necesita la estrategia de dead-letter-queue del conector de Kafka —
 * ver la clase equivalente en order-service ({@code OrderCommandMessageSerializer})
 * para la explicación completa de por qué existe.
 */
public class InventoryCommandMessageSerializer extends ObjectMapperSerializer<InventoryCommandMessage> {
}
