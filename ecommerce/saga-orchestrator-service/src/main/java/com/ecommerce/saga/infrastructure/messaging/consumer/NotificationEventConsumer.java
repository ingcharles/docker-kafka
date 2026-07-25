package com.ecommerce.saga.infrastructure.messaging.consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.smallrye.mutiny.Uni;

import com.ecommerce.saga.application.event.NotificationEventMessage;
import com.ecommerce.saga.application.service.SagaOrchestratorService;

@ApplicationScoped
public class NotificationEventConsumer {

    private final SagaOrchestratorService orchestratorService;

    @Inject
    public NotificationEventConsumer(SagaOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @Incoming("notification-events-in")
    public Uni<Void> onEvent(NotificationEventMessage event) {
        return orchestratorService.handleNotificationEvent(event);
    }
}
