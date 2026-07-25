package com.ecommerce.order.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;

import com.ecommerce.order.application.event.OrderCommandMessage;

/**
 * Esta clase existe porque la estrategia de "dead-letter-queue" (cola de
 * cartas muertas) del conector de Kafka de SmallRye, configurada en el canal
 * {@code order-commands-in}, la necesita: el conector deduce el nombre de
 * esta clase por convención a partir de {@link OrderCommandMessageDeserializer}
 * (cambiando el sufijo Deserializer por Serializer), para poder volver a
 * serializar un mensaje justo antes de reenviarlo al tópico de DLQ cuando su
 * procesamiento falló.
 */
public class OrderCommandMessageSerializer extends ObjectMapperSerializer<OrderCommandMessage> {
}
