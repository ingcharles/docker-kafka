package com.facturacion.identidad.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;

import com.facturacion.identidad.application.event.EstablecimientoPuntoEmisionEventMessage;

/** La estrategia dead-letter-queue del canal la necesita por convención de nombres. */
public class EstablecimientoPuntoEmisionEventMessageSerializer
        extends ObjectMapperSerializer<EstablecimientoPuntoEmisionEventMessage> {
}
