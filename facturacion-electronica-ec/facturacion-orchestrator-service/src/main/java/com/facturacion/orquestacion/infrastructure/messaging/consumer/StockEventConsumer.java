package com.facturacion.orquestacion.infrastructure.messaging.consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;

import com.facturacion.orquestacion.application.event.StockEventMessage;
import com.facturacion.orquestacion.application.service.SagaOrchestratorService;

/** Respuestas de inventario-service a los comandos de stock (reservado, no disponible, confirmado, liberado). */
@ApplicationScoped
public class StockEventConsumer {

    private static final Logger LOG = Logger.getLogger(StockEventConsumer.class);

    private final SagaOrchestratorService orchestratorService;

    @Inject
    public StockEventConsumer(SagaOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @Incoming("inventario-events-in")
    public Uni<Void> procesarEvento(StockEventMessage event) {
        return orchestratorService.procesarEventoStock(event)
            .invoke(resultado -> resultado.registrar(LOG, "procesarEventoStock", event.eventId(), event.comprobanteId()))
            .replaceWithVoid();
    }
}
