package com.facturacion.sri.infrastructure.messaging.consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.facturacion.sri.application.event.EnviarComprobanteCommand;
import com.facturacion.sri.application.service.SriConectorService;
import com.facturacion.sri.infrastructure.inbox.ProcessedMessageEntity;
import com.facturacion.sri.infrastructure.inbox.ProcessedMessageRepository;

/**
 * Arranca el envío al SRI cuando facturacion-orchestrator-service lo pide
 * (después de que firma-electronica-service confirmó la firma). Idempotente
 * gracias al inbox — mismo patrón que el resto de los consumidores de
 * comandos del stack.
 */
@ApplicationScoped
public class EnviarComprobanteCommandConsumer {

    private static final Logger LOG = Logger.getLogger(EnviarComprobanteCommandConsumer.class);

    private final SriConectorService sriConectorService;
    private final ProcessedMessageRepository processedMessageRepository;

    @Inject
    public EnviarComprobanteCommandConsumer(SriConectorService sriConectorService,
            ProcessedMessageRepository processedMessageRepository) {
        this.sriConectorService = sriConectorService;
        this.processedMessageRepository = processedMessageRepository;
    }

    @Incoming("sri-comandos-in")
    @WithTransaction
    public Uni<Void> procesarComando(EnviarComprobanteCommand command) {
        // Un comando sin identificadores no se puede procesar ni dejar rastro en la base: se rechaza con un motivo
        // claro y SmallRye lo envía al DLQ, en vez de fallar con un error de restricción de la base.
        if (command == null || command.eventId() == null || command.comprobanteId() == null) {
            return Uni.createFrom().failure(new IllegalArgumentException(
                "Comando de envío al SRI inválido: falta eventId o comprobanteId (" + command + ")"));
        }
        return processedMessageRepository.alreadyProcessed(command.eventId())
            .onItem().transformToUni(processed -> {
                if (Boolean.TRUE.equals(processed)) {
                    LOG.infof("Se descarta el comando %s (comprobante %s) — ya se procesó antes",
                        command.eventId(), command.comprobanteId());
                    return Uni.createFrom().voidItem();
                }
                return sriConectorService.procesarComando(command)
                    .chain(() -> processedMessageRepository.persist(ProcessedMessageEntity.of(command.eventId())))
                    .replaceWithVoid();
            });
    }
}
