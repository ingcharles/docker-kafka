package com.facturacion.orquestacion.infrastructure.messaging.consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;

import com.facturacion.orquestacion.application.event.ComprobanteEventMessage;
import com.facturacion.orquestacion.application.service.SagaOrchestratorService;

@ApplicationScoped
public class ComprobanteEventConsumer {

    private static final Logger LOG = Logger.getLogger(ComprobanteEventConsumer.class);

    private final SagaOrchestratorService orchestratorService;

    @Inject
    public ComprobanteEventConsumer(SagaOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @Incoming("comprobante-events-in")
    public Uni<Void> procesarEvento(ComprobanteEventMessage event) {
        return orchestratorService.procesarEventoComprobante(event)
            .invoke(resultado -> resultado.registrar(LOG, "procesarEventoComprobante", event.eventId(), event.comprobanteId()))
            .replaceWithVoid();
    }
}
