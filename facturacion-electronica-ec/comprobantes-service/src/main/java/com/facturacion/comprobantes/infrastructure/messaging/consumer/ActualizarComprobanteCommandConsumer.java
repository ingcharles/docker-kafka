package com.facturacion.comprobantes.infrastructure.messaging.consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.facturacion.commons.error.LogCodificado;
import com.facturacion.commons.messaging.LogFallosMensajeria;
import com.facturacion.comprobantes.application.event.ActualizarComprobanteCommand;
import com.facturacion.comprobantes.application.service.ActualizacionComprobanteService;
import com.facturacion.comprobantes.infrastructure.inbox.ProcessedMessageEntity;
import com.facturacion.comprobantes.infrastructure.inbox.ProcessedMessageRepository;
import com.facturacion.comprobantes.shared.error.CodigoLog;

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

    @LogFallosMensajeria
    @Incoming("comprobante-comandos-in")
    @WithTransaction
    public Uni<Void> procesarComando(ActualizarComprobanteCommand command) {
        return processedMessageRepository.alreadyProcessed(command.eventId())
            .onItem().transformToUni(processed -> {
                if (Boolean.TRUE.equals(processed)) {
                    LogCodificado.info(LOG, CodigoLog.COMANDO_DUPLICADO_DESCARTADO, "comprobanteId=" + command.comprobanteId(),
                        command.eventId());
                    return Uni.createFrom().voidItem();
                }
                return actualizacionService.aplicarComando(command)
                    .invoke(resultado -> LogCodificado.info(LOG, CodigoLog.COMANDO_APLICADO, "comprobanteId=" + command.comprobanteId(),
                        command.tipoComando(), command.eventId(), resultado))
                    .chain(() -> processedMessageRepository.persist(ProcessedMessageEntity.of(command.eventId())))
                    .replaceWithVoid();
            });
    }
}
