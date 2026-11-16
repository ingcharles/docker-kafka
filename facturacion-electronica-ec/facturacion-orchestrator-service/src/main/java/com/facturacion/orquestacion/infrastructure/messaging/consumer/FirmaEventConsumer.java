package com.facturacion.orquestacion.infrastructure.messaging.consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;

import com.facturacion.orquestacion.application.event.ComprobanteFirmadoEventMessage;
import com.facturacion.orquestacion.application.service.SagaOrchestratorService;

@ApplicationScoped
public class FirmaEventConsumer {

    private static final Logger LOG = Logger.getLogger(FirmaEventConsumer.class);

    private final SagaOrchestratorService orchestratorService;

    @Inject
    public FirmaEventConsumer(SagaOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @Incoming("firma-events-in")
    public Uni<Void> procesarEvento(ComprobanteFirmadoEventMessage event) {
        return orchestratorService.procesarEventoFirma(event)
            .invoke(resultado -> resultado.registrar(LOG, "procesarEventoFirma", event.eventId(), event.comprobanteId()))
            .replaceWithVoid();
    }
}
