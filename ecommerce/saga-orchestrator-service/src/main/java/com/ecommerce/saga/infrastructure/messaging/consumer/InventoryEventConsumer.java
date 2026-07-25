package com.ecommerce.saga.infrastructure.messaging.consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.smallrye.mutiny.Uni;

import com.ecommerce.saga.application.event.InventoryEventMessage;
import com.ecommerce.saga.application.service.SagaOrchestratorService;

@ApplicationScoped
public class InventoryEventConsumer {

    private final SagaOrchestratorService orchestratorService;

    @Inject
    public InventoryEventConsumer(SagaOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @Incoming("inventory-events-in")
    public Uni<Void> onEvent(InventoryEventMessage event) {
        return orchestratorService.handleInventoryEvent(event);
    }
}
