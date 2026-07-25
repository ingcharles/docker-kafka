package com.ecommerce.saga.infrastructure.messaging.consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.smallrye.mutiny.Uni;

import com.ecommerce.saga.application.event.PaymentEventMessage;
import com.ecommerce.saga.application.service.SagaOrchestratorService;

@ApplicationScoped
public class PaymentEventConsumer {

    private final SagaOrchestratorService orchestratorService;

    @Inject
    public PaymentEventConsumer(SagaOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @Incoming("payment-events-in")
    public Uni<Void> onEvent(PaymentEventMessage event) {
        return orchestratorService.handlePaymentEvent(event);
    }
}
