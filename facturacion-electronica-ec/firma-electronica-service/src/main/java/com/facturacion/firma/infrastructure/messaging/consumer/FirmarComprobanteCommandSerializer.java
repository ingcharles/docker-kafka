package com.facturacion.firma.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;

import com.facturacion.firma.application.event.FirmarComprobanteCommand;

/**
 * La estrategia dead-letter-queue del canal {@code firma-comandos-in} la
 * necesita por convención de nombres a partir de
 * {@link FirmarComprobanteCommandDeserializer} — ver el mismo comentario en
 * {@code OrderCommandMessageSerializer} de order-service.
 */
public class FirmarComprobanteCommandSerializer extends ObjectMapperSerializer<FirmarComprobanteCommand> {
}
