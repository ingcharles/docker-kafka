package com.facturacion.orquestacion.infrastructure.messaging.consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.smallrye.mutiny.Uni;

import com.facturacion.orquestacion.application.event.NotificacionResultadoEventMessage;
import com.facturacion.orquestacion.application.service.SagaOrchestratorService;

@ApplicationScoped
public class NotificacionEventConsumer {

    private final SagaOrchestratorService orchestratorService;

    @Inject
    public NotificacionEventConsumer(SagaOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @Incoming("notificacion-events-in")
    public Uni<Void> onEvent(NotificacionResultadoEventMessage event) {
        return orchestratorService.handleNotificacionEvent(event);
    }
}
