package com.facturacion.notificacion.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

import com.facturacion.notificacion.application.event.ConfiguracionEmpresaEventMessage;

public class ConfiguracionEmpresaEventMessageDeserializer
        extends ObjectMapperDeserializer<ConfiguracionEmpresaEventMessage> {

    public ConfiguracionEmpresaEventMessageDeserializer() {
        super(ConfiguracionEmpresaEventMessage.class);
    }
}
