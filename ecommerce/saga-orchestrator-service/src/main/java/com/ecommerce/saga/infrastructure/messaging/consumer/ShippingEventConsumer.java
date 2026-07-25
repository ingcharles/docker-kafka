package com.ecommerce.saga.infrastructure.messaging.consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.smallrye.mutiny.Uni;

import com.ecommerce.saga.application.event.ShippingEventMessage;
import com.ecommerce.saga.application.service.SagaOrchestratorService;

@ApplicationScoped
public class ShippingEventConsumer {

    private final SagaOrchestratorService orchestratorService;

    @Inject
    public ShippingEventConsumer(SagaOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @Incoming("shipping-events-in")
    public Uni<Void> onEvent(ShippingEventMessage event) {
        return orchestratorService.handleShippingEvent(event);
    }
}
