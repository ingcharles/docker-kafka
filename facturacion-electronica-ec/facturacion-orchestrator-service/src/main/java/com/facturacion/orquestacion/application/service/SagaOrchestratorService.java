package com.facturacion.orquestacion.application.service;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.facturacion.orquestacion.application.event.ActualizarComprobanteCommand;
import com.facturacion.orquestacion.application.event.ComprobanteEventMessage;
import com.facturacion.orquestacion.application.event.ComprobanteFirmadoEventMessage;
import com.facturacion.orquestacion.application.event.EnviarComprobanteCommand;
import com.facturacion.orquestacion.application.event.FirmarComprobanteCommand;
import com.facturacion.orquestacion.application.event.NotificacionResultadoEventMessage;
import com.facturacion.orquestacion.application.event.SriResultadoEventMessage;
import com.facturacion.orquestacion.domain.model.entity.SagaInstance;
import com.facturacion.orquestacion.domain.model.entity.SagaStep;
import com.facturacion.orquestacion.domain.model.enums.SagaState;
import com.facturacion.orquestacion.domain.repository.SagaInstanceRepository;
import com.facturacion.orquestacion.domain.repository.SagaStepRepository;
import com.facturacion.orquestacion.infrastructure.inbox.ProcessedMessageEntity;
import com.facturacion.orquestacion.infrastructure.inbox.ProcessedMessageRepository;
import com.facturacion.orquestacion.infrastructure.outbox.OutboxEventEntity;
import com.facturacion.orquestacion.infrastructure.outbox.OutboxEventRepository;
import com.facturacion.orquestacion.shared.constant.MessagingConstants;

/**
 * La única pieza de todo el sistema que conoce la saga completa. Todos los
 * manejadores siguen la misma estructura que el orquestador de ecommerce/:
 * revisar el inbox, aplicar la transición de estado (que no hace nada si la
 * saga no está en el estado de origen esperado — por ejemplo, un evento
 * duplicado después de un reintento de Kafka), emitir lo que corresponda a
 * continuación, y marcar el inbox.
 * <p>
 * A diferencia de la saga de ecommerce, acá no hay compensación: firmar,
 * enviar al SRI y notificar no tienen efecto colateral que deshacer, y una
 * vez que el SRI autoriza, el comprobante es legalmente válido pase lo que
 * pase después (una notificación fallida no revierte nada — ver
 * {@link #onNotificacionFallida}).
 */
@ApplicationScoped
public class SagaOrchestratorService {

    private final SagaInstanceRepository sagaInstanceRepository;
    private final SagaStepRepository sagaStepRepository;
    private final OutboxEventRepository outboxRepository;
    private final ProcessedMessageRepository processedMessageRepository;
    private final ObjectMapper objectMapper;

    @Inject
    public SagaOrchestratorService(SagaInstanceRepository sagaInstanceRepository,
            SagaStepRepository sagaStepRepository, OutboxEventRepository outboxRepository,
            ProcessedMessageRepository processedMessageRepository, ObjectMapper objectMapper) {
        this.sagaInstanceRepository = sagaInstanceRepository;
        this.sagaStepRepository = sagaStepRepository;
        this.outboxRepository = outboxRepository;
        this.processedMessageRepository = processedMessageRepository;
        this.objectMapper = objectMapper;
    }

    @WithTransaction
    public Uni<Void> handleComprobanteEvent(ComprobanteEventMessage event) {
        // ComprobanteAutorizado también viaja por este tópico, pero lo
        // consume directo notificacion-service — acá se ignora sin más.
        if (!MessagingConstants.EVENT_TYPE_COMPROBANTE_CREADO.equals(event.eventType())) {
            return Uni.createFrom().voidItem();
        }
        return ifNotProcessed(event.eventId(), () -> {
            SagaInstance saga = SagaInstance.start(event.comprobanteId(), event.emisorRuc(), event.claveAcceso(),
                event.ambiente());
            FirmarComprobanteCommand command = new FirmarComprobanteCommand(UUID.randomUUID(), event.comprobanteId(),
                event.emisorRuc(), event.xmlGenerado(), Instant.now());

            return sagaInstanceRepository.persist(saga)
                .chain(() -> logReceived(event.comprobanteId(), MessagingConstants.EVENT_TYPE_COMPROBANTE_CREADO))
                .chain(() -> emit(event.comprobanteId(), "FirmarComprobante", MessagingConstants.FIRMA_COMMANDS_TOPIC,
                    command))
                .chain(() -> markProcessed(event.eventId()));
        });
    }

    @WithTransaction
    public Uni<Void> handleFirmaEvent(ComprobanteFirmadoEventMessage event) {
        return ifNotProcessed(event.eventId(), () -> logReceived(event.comprobanteId(), event.eventType())
            .chain(() -> switch (event.eventType()) {
                case MessagingConstants.EVENT_TYPE_COMPROBANTE_FIRMADO -> onComprobanteFirmado(event);
                case MessagingConstants.EVENT_TYPE_FIRMA_FALLIDA ->
                    onFirmaFallida(event.comprobanteId(), event.motivoError());
                default -> Uni.createFrom().voidItem();
            })
            .chain(() -> markProcessed(event.eventId())));
    }

    @WithTransaction
    public Uni<Void> handleSriEvent(SriResultadoEventMessage event) {
        return ifNotProcessed(event.eventId(), () -> logReceived(event.comprobanteId(), event.eventType())
            .chain(() -> switch (event.eventType()) {
                case MessagingConstants.EVENT_TYPE_SRI_AUTORIZADO -> onSriAutorizado(event);
                case MessagingConstants.EVENT_TYPE_SRI_NO_AUTORIZADO ->
                    onSriRechazo(event, SagaState.NO_AUTORIZADO, MessagingConstants.COMANDO_MARCAR_NO_AUTORIZADO);
                case MessagingConstants.EVENT_TYPE_SRI_RECHAZADO_RECEPCION ->
                    onSriRechazo(event, SagaState.RECHAZADO_RECEPCION,
                        MessagingConstants.COMANDO_MARCAR_NO_AUTORIZADO);
                // SriRecibido: solo queda registrado en la bitácora, no dispara ninguna
                // transición — el poller de autorización sigue trabajando por su cuenta.
                default -> Uni.createFrom().voidItem();
            })
            .chain(() -> markProcessed(event.eventId())));
    }

    @WithTransaction
    public Uni<Void> handleNotificacionEvent(NotificacionResultadoEventMessage event) {
        return ifNotProcessed(event.eventId(), () -> logReceived(event.comprobanteId(), event.eventType())
            .chain(() -> switch (event.eventType()) {
                case MessagingConstants.EVENT_TYPE_NOTIFICACION_ENVIADA -> onNotificacionEnviada(event.comprobanteId());
                case MessagingConstants.EVENT_TYPE_NOTIFICACION_FALLIDA ->
                    onNotificacionFallida(event.comprobanteId(), event.motivoError());
                default -> Uni.createFrom().voidItem();
            })
            .chain(() -> markProcessed(event.eventId())));
    }

    /**
     * Vuelve a publicar el comando enviado más recientemente (estado SENT)
     * para esta saga, usando exactamente el mismo contenido — y por lo
     * tanto el mismo {@code eventId} incrustado — que se emitió la primera
     * vez. Es la idempotencia del servicio destino la que después decide si
     * esto es un reintento genuino o si no hace falta nada.
     */
    @WithTransaction
    public Uni<Void> retryLastCommand(UUID comprobanteId) {
        return sagaStepRepository.findLastSent(comprobanteId)
            .onItem().transformToUni(lastSent -> {
                if (lastSent == null) {
                    return Uni.createFrom().voidItem();
                }
                return outboxRepository.persist(OutboxEventEntity.of(comprobanteId, lastSent.getStepName(),
                        lastSent.getTopic(), lastSent.getPayload()))
                    .replaceWithVoid();
            });
    }

    // --- Camino feliz -----------------------------------------------------

    private Uni<Void> onComprobanteFirmado(ComprobanteFirmadoEventMessage event) {
        return sagaInstanceRepository.updateState(event.comprobanteId(), SagaState.FIRMADO, null, SagaState.CREADO)
            .onItem().transformToUni(saga -> {
                if (saga == null) {
                    return Uni.createFrom().voidItem();
                }
                ActualizarComprobanteCommand marcarFirmado = new ActualizarComprobanteCommand(UUID.randomUUID(),
                    event.comprobanteId(), MessagingConstants.COMANDO_MARCAR_FIRMADO, event.xmlFirmado(), null, null,
                    null, null, Instant.now());
                EnviarComprobanteCommand enviarSri = new EnviarComprobanteCommand(UUID.randomUUID(),
                    event.comprobanteId(), saga.getClaveAcceso(), saga.getAmbiente(), event.xmlFirmado(),
                    Instant.now());
                return emit(event.comprobanteId(), MessagingConstants.COMANDO_MARCAR_FIRMADO,
                        MessagingConstants.COMPROBANTE_COMMANDS_TOPIC, marcarFirmado)
                    .chain(() -> emit(event.comprobanteId(), "EnviarComprobante",
                        MessagingConstants.SRI_COMMANDS_TOPIC, enviarSri));
            });
    }

    private Uni<Void> onSriAutorizado(SriResultadoEventMessage event) {
        return sagaInstanceRepository.updateState(event.comprobanteId(), SagaState.AUTORIZADO, null,
                SagaState.FIRMADO)
            .onItem().transformToUni(saga -> {
                if (saga == null) {
                    return Uni.createFrom().voidItem();
                }
                ActualizarComprobanteCommand command = new ActualizarComprobanteCommand(UUID.randomUUID(),
                    event.comprobanteId(), MessagingConstants.COMANDO_MARCAR_AUTORIZADO, null,
                    event.xmlAutorizado(), event.numeroAutorizacion(), event.fechaAutorizacion(), null,
                    Instant.now());
                // A partir de acá comprobantes-service genera el RIDE y publica
                // ComprobanteAutorizado por su cuenta — notificacion-service lo
                // consume directo, este orquestador no tiene que hacer nada más
                // para que el correo salga.
                return emit(event.comprobanteId(), MessagingConstants.COMANDO_MARCAR_AUTORIZADO,
                    MessagingConstants.COMPROBANTE_COMMANDS_TOPIC, command);
            });
    }

    private Uni<Void> onNotificacionEnviada(UUID comprobanteId) {
        return sagaInstanceRepository.updateState(comprobanteId, SagaState.NOTIFICADO, null, SagaState.AUTORIZADO)
            .onItem().transformToUni(saga -> {
                if (saga == null) {
                    return Uni.createFrom().voidItem();
                }
                ActualizarComprobanteCommand command = new ActualizarComprobanteCommand(UUID.randomUUID(),
                    comprobanteId, MessagingConstants.COMANDO_MARCAR_NOTIFICADO, null, null, null, null, null,
                    Instant.now());
                return emit(comprobanteId, MessagingConstants.COMANDO_MARCAR_NOTIFICADO,
                    MessagingConstants.COMPROBANTE_COMMANDS_TOPIC, command);
            });
    }

    // --- Terminales de error (sin compensación) ----------------------------

    private Uni<Void> onFirmaFallida(UUID comprobanteId, String motivoError) {
        return sagaInstanceRepository.updateState(comprobanteId, SagaState.FIRMA_FALLIDA, motivoError,
                SagaState.CREADO)
            .onItem().transformToUni(saga -> {
                if (saga == null) {
                    return Uni.createFrom().voidItem();
                }
                ActualizarComprobanteCommand command = new ActualizarComprobanteCommand(UUID.randomUUID(),
                    comprobanteId, MessagingConstants.COMANDO_MARCAR_FIRMA_FALLIDA, null, null, null, null,
                    motivoError, Instant.now());
                return emit(comprobanteId, MessagingConstants.COMANDO_MARCAR_FIRMA_FALLIDA,
                    MessagingConstants.COMPROBANTE_COMMANDS_TOPIC, command);
            });
    }

    private Uni<Void> onSriRechazo(SriResultadoEventMessage event, SagaState estadoDestino, String tipoComando) {
        return sagaInstanceRepository.updateState(event.comprobanteId(), estadoDestino, event.motivoRechazo(),
                SagaState.FIRMADO)
            .onItem().transformToUni(saga -> {
                if (saga == null) {
                    return Uni.createFrom().voidItem();
                }
                ActualizarComprobanteCommand command = new ActualizarComprobanteCommand(UUID.randomUUID(),
                    event.comprobanteId(), tipoComando, null, null, null, null, event.motivoRechazo(), Instant.now());
                return emit(event.comprobanteId(), tipoComando, MessagingConstants.COMPROBANTE_COMMANDS_TOPIC,
                    command);
            });
    }

    /**
     * Una notificación fallida no revierte nada: para este punto el
     * comprobante ya está autorizado por el SRI (legalmente válido pase lo
     * que pase con el correo), así que no se manda ningún comando a
     * comprobantes-service — solo queda registrado acá, en la saga, para
     * que un humano (o un futuro mecanismo de reintento) lo pueda ver.
     */
    private Uni<Void> onNotificacionFallida(UUID comprobanteId, String motivoError) {
        return sagaInstanceRepository.updateState(comprobanteId, SagaState.NOTIFICACION_FALLIDA, motivoError,
                SagaState.AUTORIZADO)
            .replaceWithVoid();
    }

    // --- Ayudantes compartidos ----------------------------------------------

    private Uni<Void> ifNotProcessed(UUID eventId, Supplier<Uni<Void>> action) {
        return processedMessageRepository.alreadyProcessed(eventId)
            .onItem().transformToUni(processed -> Boolean.TRUE.equals(processed)
                ? Uni.createFrom().voidItem()
                : action.get());
    }

    private Uni<Void> logReceived(UUID sagaId, String stepName) {
        return sagaStepRepository.persist(SagaStep.received(sagaId, stepName)).replaceWithVoid();
    }

    private Uni<Void> emit(UUID sagaId, String stepName, String topic, Object message) {
        String json = writeValueAsString(message);
        return outboxRepository.persist(OutboxEventEntity.of(sagaId, stepName, topic, json))
            .chain(() -> sagaStepRepository.persist(SagaStep.sent(sagaId, stepName, topic, json)))
            .replaceWithVoid();
    }

    private Uni<Void> markProcessed(UUID eventId) {
        return processedMessageRepository.persist(ProcessedMessageEntity.of(eventId)).replaceWithVoid();
    }

    private String writeValueAsString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el payload del outbox", e);
        }
    }
}
