package com.facturacion.comprobantes.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;

import com.facturacion.comprobantes.application.event.EstablecimientoPuntoEmisionEventMessage;

/** La estrategia dead-letter-queue del canal la necesita por convención de nombres. */
public class EstablecimientoPuntoEmisionEventMessageSerializer
        extends ObjectMapperSerializer<EstablecimientoPuntoEmisionEventMessage> {
}
