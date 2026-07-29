package com.facturacion.comprobantes.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;

import com.facturacion.comprobantes.application.event.ConfiguracionEmpresaEventMessage;

/** La estrategia dead-letter-queue del canal la necesita por convención de nombres. */
public class ConfiguracionEmpresaEventMessageSerializer
        extends ObjectMapperSerializer<ConfiguracionEmpresaEventMessage> {
}
