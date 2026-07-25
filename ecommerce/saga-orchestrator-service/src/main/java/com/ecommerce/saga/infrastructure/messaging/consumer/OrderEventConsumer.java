package com.ecommerce.saga.infrastructure.messaging.consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.smallrye.mutiny.Uni;

import com.ecommerce.saga.application.event.OrderEventMessage;
import com.ecommerce.saga.application.service.SagaOrchestratorService;

@ApplicationScoped
public class OrderEventConsumer {

    private final SagaOrchestratorService orchestratorService;

    @Inject
    public OrderEventConsumer(SagaOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @Incoming("order-events-in")
    public Uni<Void> onEvent(OrderEventMessage event) {
        return orchestratorService.handleOrderCreated(event);
    }
}
