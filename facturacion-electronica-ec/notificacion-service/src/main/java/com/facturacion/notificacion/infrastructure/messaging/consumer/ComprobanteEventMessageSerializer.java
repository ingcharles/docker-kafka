package com.facturacion.notificacion.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;

import com.facturacion.notificacion.application.event.ComprobanteEventMessage;

/** La estrategia dead-letter-queue del canal {@code comprobante-events-in} la necesita por convención de nombres. */
public class ComprobanteEventMessageSerializer extends ObjectMapperSerializer<ComprobanteEventMessage> {
}
