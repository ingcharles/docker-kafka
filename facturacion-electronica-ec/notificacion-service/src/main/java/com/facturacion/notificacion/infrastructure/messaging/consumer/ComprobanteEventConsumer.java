package com.facturacion.notificacion.infrastructure.messaging.consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.facturacion.notificacion.application.event.ComprobanteEventMessage;
import com.facturacion.notificacion.application.service.NotificacionService;
import com.facturacion.notificacion.infrastructure.inbox.ProcessedMessageEntity;
import com.facturacion.notificacion.infrastructure.inbox.ProcessedMessageRepository;
import com.facturacion.notificacion.shared.constant.MessagingConstants;

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

    @Incoming("comprobante-events-in")
    @WithTransaction
    public Uni<Void> onEvent(ComprobanteEventMessage evento) {
        if (!MessagingConstants.EVENT_TYPE_COMPROBANTE_AUTORIZADO.equals(evento.eventType())) {
            return Uni.createFrom().voidItem();
        }
        if (evento.compradorCorreo() == null || evento.compradorCorreo().isBlank()) {
            LOG.warnf("ComprobanteAutorizado %s sin compradorCorreo — no debería haberse publicado, se descarta",
                evento.comprobanteId());
            return Uni.createFrom().voidItem();
        }
        return processedMessageRepository.alreadyProcessed(evento.eventId())
            .onItem().transformToUni(processed -> {
                if (Boolean.TRUE.equals(processed)) {
                    LOG.infof("Se descarta el evento %s (comprobante %s) — ya se procesó antes", evento.eventId(),
                        evento.comprobanteId());
                    return Uni.createFrom().voidItem();
                }
                return notificacionService.procesarComprobanteAutorizado(evento)
                    .chain(() -> processedMessageRepository.persist(ProcessedMessageEntity.of(evento.eventId())))
                    .replaceWithVoid();
            });
    }
}
