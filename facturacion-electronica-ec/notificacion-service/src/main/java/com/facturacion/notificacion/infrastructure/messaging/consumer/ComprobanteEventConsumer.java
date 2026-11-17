package com.facturacion.notificacion.infrastructure.messaging.consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.facturacion.commons.error.LogCodificado;
import com.facturacion.commons.messaging.LogFallosMensajeria;
import com.facturacion.notificacion.application.event.ComprobanteEventMessage;
import com.facturacion.notificacion.application.service.NotificacionService;
import com.facturacion.notificacion.infrastructure.inbox.ProcessedMessageEntity;
import com.facturacion.notificacion.infrastructure.inbox.ProcessedMessageRepository;
import com.facturacion.notificacion.shared.constant.MessagingConstants;
import com.facturacion.notificacion.shared.error.CodigoLog;

/**
 * {@code facturacion.comprobante.eventos.v1} lleva más de un tipo de
 * evento — este consumidor descarta silenciosamente cualquiera que no sea
 * {@code ComprobanteAutorizado} (hoy: {@code ComprobanteCreado}), y dentro
 * de ese tipo, los que no traigan {@code compradorCorreo} (comprobantes-service
 * ya filtra eso antes de publicar, pero se revalida acá por si acaso).
 * Idempotente vía inbox — mismo patrón que el resto del stack.
 */
@ApplicationScoped
public class ComprobanteEventConsumer {

    private static final Logger LOG = Logger.getLogger(ComprobanteEventConsumer.class);

    private final NotificacionService notificacionService;
    private final ProcessedMessageRepository processedMessageRepository;

    @Inject
    public ComprobanteEventConsumer(NotificacionService notificacionService,
            ProcessedMessageRepository processedMessageRepository) {
        this.notificacionService = notificacionService;
        this.processedMessageRepository = processedMessageRepository;
    }

    @LogFallosMensajeria
    @Incoming("comprobante-events-in")
    @WithTransaction
    public Uni<Void> procesarEvento(ComprobanteEventMessage evento) {
        if (!MessagingConstants.EVENT_TYPE_COMPROBANTE_AUTORIZADO.equals(evento.eventType())) {
            return Uni.createFrom().voidItem();
        }
        if (evento.compradorCorreo() == null || evento.compradorCorreo().isBlank()) {
            LogCodificado.warn(LOG, CodigoLog.SIN_CORREO_RECEPTOR, "comprobanteId=" + evento.comprobanteId());
            return Uni.createFrom().voidItem();
        }
        return processedMessageRepository.alreadyProcessed(evento.eventId())
            .onItem().transformToUni(processed -> {
                if (Boolean.TRUE.equals(processed)) {
                    LogCodificado.info(LOG, CodigoLog.EVENTO_DUPLICADO_DESCARTADO, "comprobanteId=" + evento.comprobanteId(),
                        evento.eventId());
                    return Uni.createFrom().voidItem();
                }
                return notificacionService.procesarComprobanteAutorizado(evento)
                    .chain(() -> processedMessageRepository.persist(ProcessedMessageEntity.of(evento.eventId())))
                    .replaceWithVoid();
            });
    }
}
