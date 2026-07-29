package com.facturacion.notificacion.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;

import com.facturacion.notificacion.application.event.ConfiguracionEmpresaEventMessage;

/** La estrategia dead-letter-queue del canal la necesita por convención de nombres. */
public class ConfiguracionEmpresaEventMessageSerializer
        extends ObjectMapperSerializer<ConfiguracionEmpresaEventMessage> {
}
