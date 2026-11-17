package com.facturacion.orquestacion.infrastructure.messaging.consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;

import com.facturacion.commons.messaging.LogFallosMensajeria;
import com.facturacion.orquestacion.application.event.NotificacionResultadoEventMessage;
import com.facturacion.orquestacion.application.service.SagaOrchestratorService;

@ApplicationScoped
public class NotificacionEventConsumer {

    private static final Logger LOG = Logger.getLogger(NotificacionEventConsumer.class);

    private final SagaOrchestratorService orchestratorService;

    @Inject
    public NotificacionEventConsumer(SagaOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @LogFallosMensajeria
    @Incoming("notificacion-events-in")
    public Uni<Void> procesarEvento(NotificacionResultadoEventMessage event) {
        return orchestratorService.procesarEventoNotificacion(event)
            .invoke(resultado -> resultado.registrar(LOG, "procesarEventoNotificacion", event.eventId(), event.comprobanteId()))
            .replaceWithVoid();
    }
}
