package com.facturacion.sri.infrastructure.messaging.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

import com.facturacion.sri.application.event.ConfiguracionEmpresaEventMessage;

public class ConfiguracionEmpresaEventMessageDeserializer
        extends ObjectMapperDeserializer<ConfiguracionEmpresaEventMessage> {

    public ConfiguracionEmpresaEventMessageDeserializer() {
        super(ConfiguracionEmpresaEventMessage.class);
    }
}
