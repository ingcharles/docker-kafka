package com.facturacion.orquestacion.infrastructure.messaging.consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;

import com.facturacion.orquestacion.application.event.SriResultadoEventMessage;
import com.facturacion.orquestacion.application.service.SagaOrchestratorService;

@ApplicationScoped
public class SriEventConsumer {

    private static final Logger LOG = Logger.getLogger(SriEventConsumer.class);

    private final SagaOrchestratorService orchestratorService;

    @Inject
    public SriEventConsumer(SagaOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @Incoming("sri-events-in")
    public Uni<Void> procesarEvento(SriResultadoEventMessage event) {
        return orchestratorService.procesarEventoSri(event)
            .invoke(resultado -> resultado.registrar(LOG, "procesarEventoSri", event.eventId(), event.comprobanteId()))
            .replaceWithVoid();
    }
}
