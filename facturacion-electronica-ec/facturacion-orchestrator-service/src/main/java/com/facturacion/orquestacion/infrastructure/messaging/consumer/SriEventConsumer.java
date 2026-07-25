package com.facturacion.orquestacion.infrastructure.messaging.consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.smallrye.mutiny.Uni;

import com.facturacion.orquestacion.application.event.SriResultadoEventMessage;
import com.facturacion.orquestacion.application.service.SagaOrchestratorService;

@ApplicationScoped
public class SriEventConsumer {

    private final SagaOrchestratorService orchestratorService;

    @Inject
    public SriEventConsumer(SagaOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @Incoming("sri-events-in")
    public Uni<Void> onEvent(SriResultadoEventMessage event) {
        return orchestratorService.handleSriEvent(event);
    }
}
