package com.facturacion.comprobantes.infrastructure.messaging.consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.facturacion.comprobantes.application.event.ActualizarComprobanteCommand;
import com.facturacion.comprobantes.application.service.ActualizacionComprobanteService;
import com.facturacion.comprobantes.infrastructure.inbox.ProcessedMessageEntity;
import com.facturacion.comprobantes.infrastructure.inbox.ProcessedMessageRepository;

/**
 * Aplica las transiciones de estado que manda facturacion-orchestrator-service
 * a medida que avanza la saga (firmado, autorizado, etc.). Idempotente
 * gracias al inbox — mismo patrón que el resto de los consumidores de
 * comandos del stack.
 */
@ApplicationScoped
public class ActualizarComprobanteCommandConsumer {

    private static final Logger LOG = Logger.getLogger(ActualizarComprobanteCommandConsumer.class);

    private final ActualizacionComprobanteService actualizacionService;
    private final ProcessedMessageRepository processedMessageRepository;

    @Inject
    public ActualizarComprobanteCommandConsumer(ActualizacionComprobanteService actualizacionService,
            ProcessedMessageRepository processedMessageRepository) {
        this.actualizacionService = actualizacionService;
        this.processedMessageRepository = processedMessageRepository;
    }

    @Incoming("comprobante-comandos-in")
    @WithTransaction
    public Uni<Void> onCommand(ActualizarComprobanteCommand command) {
        return processedMessageRepository.alreadyProcessed(command.eventId())
            .onItem().transformToUni(processed -> {
                if (Boolean.TRUE.equals(processed)) {
                    LOG.infof("Se descarta el comando %s (comprobante %s) — ya se procesó antes",
                        command.eventId(), command.comprobanteId());
                    return Uni.createFrom().voidItem();
                }
                return actualizacionService.aplicarComando(command)
                    .chain(() -> processedMessageRepository.persist(ProcessedMessageEntity.of(command.eventId())))
                    .replaceWithVoid();
            });
    }
}
