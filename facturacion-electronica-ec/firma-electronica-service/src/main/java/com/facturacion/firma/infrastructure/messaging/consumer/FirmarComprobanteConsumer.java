package com.facturacion.firma.infrastructure.messaging.consumer;

import java.time.Instant;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.facturacion.firma.application.event.ComprobanteFirmadoEventMessage;
import com.facturacion.firma.application.event.FirmarComprobanteCommand;
import com.facturacion.firma.application.service.FirmaXadesService;
import com.facturacion.firma.infrastructure.inbox.ProcessedMessageEntity;
import com.facturacion.firma.infrastructure.inbox.ProcessedMessageRepository;
import com.facturacion.firma.infrastructure.outbox.OutboxEventEntity;
import com.facturacion.firma.infrastructure.outbox.OutboxEventRepository;
import com.facturacion.firma.shared.constant.MessagingConstants;

/**
 * Firma el XML de un comprobante con el certificado activo de la empresa
 * indicado, e informa el resultado (éxito o fallo) de vuelta al orquestador
 * vía outbox. Idempotente gracias al inbox — mismo patrón que
 * {@code OrderCommandConsumer} en ecommerce/.
 */
@ApplicationScoped
public class FirmarComprobanteConsumer {

    private static final Logger LOG = Logger.getLogger(FirmarComprobanteConsumer.class);

    private final FirmaXadesService firmaXadesService;
    private final ProcessedMessageRepository processedMessageRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Inject
    public FirmarComprobanteConsumer(FirmaXadesService firmaXadesService,
            ProcessedMessageRepository processedMessageRepository, OutboxEventRepository outboxRepository,
            ObjectMapper objectMapper) {
        this.firmaXadesService = firmaXadesService;
        this.processedMessageRepository = processedMessageRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Incoming("firma-comandos-in")
    @WithTransaction
    public Uni<Void> onCommand(FirmarComprobanteCommand command) {
        return processedMessageRepository.alreadyProcessed(command.eventId())
            .onItem().transformToUni(processed -> {
                if (Boolean.TRUE.equals(processed)) {
                    LOG.infof("Se descarta el comando de firma %s (comprobante %s) — ya se procesó antes",
                        command.eventId(), command.comprobanteId());
                    return Uni.createFrom().voidItem();
                }
                return firmaXadesService.firmarXml(command.ruc(), command.xmlSinFirmar())
                    .onItem().transformToUni(xmlFirmado -> registrarResultado(command, true, xmlFirmado, null))
                    .onFailure().recoverWithUni(falla -> {
                        LOG.errorf(falla, "Falló la firma del comprobante %s", command.comprobanteId());
                        return registrarResultado(command, false, null, falla.getMessage());
                    })
                    .chain(() -> processedMessageRepository.persist(ProcessedMessageEntity.of(command.eventId())))
                    .replaceWithVoid();
            });
    }

    private Uni<Void> registrarResultado(FirmarComprobanteCommand command, boolean exitoso, String xmlFirmado,
            String motivoError) {
        String tipoEvento = exitoso
            ? MessagingConstants.EVENT_TYPE_COMPROBANTE_FIRMADO
            : MessagingConstants.EVENT_TYPE_FIRMA_FALLIDA;
        ComprobanteFirmadoEventMessage mensaje = new ComprobanteFirmadoEventMessage(
            UUID.randomUUID(), command.comprobanteId(), tipoEvento, xmlFirmado, motivoError, Instant.now());
        OutboxEventEntity evento = OutboxEventEntity.of(command.comprobanteId(), tipoEvento,
            MessagingConstants.FIRMA_EVENTS_TOPIC, writeValueAsString(mensaje));
        return outboxRepository.persist(evento).replaceWithVoid();
    }

    private String writeValueAsString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el payload del outbox", e);
        }
    }
}
