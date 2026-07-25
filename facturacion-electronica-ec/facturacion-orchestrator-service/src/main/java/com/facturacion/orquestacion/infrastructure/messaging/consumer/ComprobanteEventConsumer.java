package com.facturacion.orquestacion.infrastructure.messaging.consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.smallrye.mutiny.Uni;

import com.facturacion.orquestacion.application.event.ComprobanteEventMessage;
import com.facturacion.orquestacion.application.service.SagaOrchestratorService;

@ApplicationScoped
public class ComprobanteEventConsumer {

    private final SagaOrchestratorService orchestratorService;

    @Inject
    public ComprobanteEventConsumer(SagaOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @Incoming("comprobante-events-in")
    public Uni<Void> onEvent(ComprobanteEventMessage event) {
        return orchestratorService.handleComprobanteEvent(event);
    }
}
