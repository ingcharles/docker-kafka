package com.facturacion.orquestacion.infrastructure.messaging.consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.smallrye.mutiny.Uni;

import com.facturacion.orquestacion.application.event.ComprobanteFirmadoEventMessage;
import com.facturacion.orquestacion.application.service.SagaOrchestratorService;

@ApplicationScoped
public class FirmaEventConsumer {

    private final SagaOrchestratorService orchestratorService;

    @Inject
    public FirmaEventConsumer(SagaOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @Incoming("firma-events-in")
    public Uni<Void> onEvent(ComprobanteFirmadoEventMessage event) {
        return orchestratorService.handleFirmaEvent(event);
    }
}
