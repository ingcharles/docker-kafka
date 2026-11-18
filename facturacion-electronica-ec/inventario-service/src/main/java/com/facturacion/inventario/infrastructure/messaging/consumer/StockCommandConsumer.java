package com.facturacion.inventario.infrastructure.messaging.consumer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.facturacion.commons.error.ErrorComun;
import com.facturacion.commons.error.LogCodificado;
import com.facturacion.commons.exception.ErrorInternoException;
import com.facturacion.commons.messaging.LogFallosMensajeria;
import com.facturacion.inventario.application.event.StockCommandMessage;
import com.facturacion.inventario.application.event.StockEventMessage;
import com.facturacion.inventario.application.service.StockReservationService;
import com.facturacion.inventario.infrastructure.inbox.ProcessedMessageEntity;
import com.facturacion.inventario.infrastructure.inbox.ProcessedMessageRepository;
import com.facturacion.inventario.infrastructure.outbox.OutboxEventEntity;
import com.facturacion.inventario.infrastructure.outbox.OutboxEventRepository;
import com.facturacion.inventario.shared.constant.MessagingConstants;
import com.facturacion.inventario.shared.error.CodigoLog;

/**
 * Aplica los tres comandos que {@code facturacion-orchestrator-service}
 * envía a lo largo del ciclo de vida de un comprobante: RESERVAR (al
 * crearlo), CONFIRMAR (al autorizarse en el SRI) y LIBERAR (al rechazarse o
 * cancelarse). Idempotente gracias al inbox.
 */
@ApplicationScoped
public class StockCommandConsumer {

    private static final Logger LOG = Logger.getLogger(StockCommandConsumer.class);

    private final StockReservationService stockReservationService;
    private final ProcessedMessageRepository processedMessageRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Inject
    public StockCommandConsumer(StockReservationService stockReservationService,
            ProcessedMessageRepository processedMessageRepository, OutboxEventRepository outboxRepository,
            ObjectMapper objectMapper) {
        this.stockReservationService = stockReservationService;
        this.processedMessageRepository = processedMessageRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @LogFallosMensajeria
    @Incoming("inventario-comandos-in")
    @WithTransaction
    public Uni<Void> procesarComando(StockCommandMessage command) {
        return processedMessageRepository.esMensajeProcesado(command.eventId())
            .onItem().transformToUni(processed -> {
                if (Boolean.TRUE.equals(processed)) {
                    LogCodificado.info(LOG, CodigoLog.COMANDO_DUPLICADO_DESCARTADO, "comprobanteId=" + command.comprobanteId(),
                        command.eventId());
                    return Uni.createFrom().voidItem();
                }
                return aplicar(command)
                    .chain(() -> processedMessageRepository.persist(ProcessedMessageEntity.of(command.eventId())))
                    .replaceWithVoid();
            });
    }

    private Uni<Void> aplicar(StockCommandMessage command) {
        return switch (command.tipoComando()) {
            case MessagingConstants.COMANDO_RESERVAR -> stockReservationService
                .reservarTodos(command.comprobanteId(), command.items())
                .onItem().invoke(resultado -> {
                    if (!resultado.exitoso()) {
                        LogCodificado.warn(LOG, CodigoLog.STOCK_NO_DISPONIBLE, "comprobanteId=" + command.comprobanteId(),
                            resultado.itemsInsuficientes().size());
                    }
                })
                .onItem().transformToUni(resultado -> registrarEvento(command.comprobanteId(),
                    resultado.exitoso()
                        ? MessagingConstants.EVENT_TYPE_STOCK_RESERVADO
                        : MessagingConstants.EVENT_TYPE_STOCK_NO_DISPONIBLE,
                    resultado.itemsInsuficientes()));
            case MessagingConstants.COMANDO_CONFIRMAR -> stockReservationService
                .confirmarTodos(command.comprobanteId())
                .chain(() -> registrarEvento(command.comprobanteId(),
                    MessagingConstants.EVENT_TYPE_STOCK_CONFIRMADO, List.of()));
            case MessagingConstants.COMANDO_LIBERAR -> stockReservationService
                .liberarTodos(command.comprobanteId())
                .chain(() -> registrarEvento(command.comprobanteId(),
                    MessagingConstants.EVENT_TYPE_STOCK_LIBERADO, List.of()));
            default -> {
                LogCodificado.warn(LOG, CodigoLog.COMANDO_STOCK_DESCONOCIDO, "comprobanteId=" + command.comprobanteId(), command.tipoComando());
                yield Uni.createFrom().voidItem();
            }
        };
    }

    private Uni<Void> registrarEvento(UUID comprobanteId, String eventType, List<UUID> itemsInsuficientes) {
        StockEventMessage mensaje = new StockEventMessage(UUID.randomUUID(), comprobanteId, eventType,
            itemsInsuficientes, Instant.now());
        OutboxEventEntity evento = OutboxEventEntity.of(comprobanteId, eventType,
            MessagingConstants.INVENTARIO_EVENTS_TOPIC, serializarAJson(mensaje));
        return outboxRepository.persist(evento).replaceWithVoid();
    }

    private String serializarAJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new ErrorInternoException(e, ErrorComun.PAYLOAD_NO_SERIALIZABLE);
        }
    }
}
