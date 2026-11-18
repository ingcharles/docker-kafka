package com.facturacion.firma.infrastructure.messaging.consumer;

import java.time.Instant;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.facturacion.commons.error.ErrorComun;
import com.facturacion.commons.error.LogCodificado;
import com.facturacion.commons.exception.ErrorInternoException;
import com.facturacion.commons.exception.ErrorNegocioException;
import com.facturacion.commons.messaging.LogFallosMensajeria;
import com.facturacion.firma.application.event.ComprobanteFirmadoEventMessage;
import com.facturacion.firma.application.event.FirmarComprobanteCommand;
import com.facturacion.firma.application.service.FirmaXadesService;
import com.facturacion.firma.domain.model.entity.CertificadoDigital;
import com.facturacion.firma.infrastructure.inbox.ProcessedMessageEntity;
import com.facturacion.firma.infrastructure.inbox.ProcessedMessageRepository;
import com.facturacion.firma.infrastructure.outbox.OutboxEventEntity;
import com.facturacion.firma.infrastructure.outbox.OutboxEventRepository;
import com.facturacion.firma.shared.constant.MessagingConstants;
import com.facturacion.firma.shared.error.CodigoLog;
import com.facturacion.firma.shared.error.FirmaError;

/**
 * Firma el XML de un comprobante con el certificado activo de la empresa
 * indicado, e informa el resultado (éxito o fallo) de vuelta al orquestador
 * vía outbox. Idempotente gracias al inbox.
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

    @LogFallosMensajeria
    @Incoming("firma-comandos-in")
    public Uni<Void> procesarComando(FirmarComprobanteCommand command) {
        // Un comando sin identificadores no se puede procesar ni dejar rastro en la base (el outbox exige el id del
        // comprobante): se rechaza con un motivo claro y SmallRye lo envía al DLQ, en vez de fallar con un error de
        // restricción de la base.
        if (command == null || command.eventId() == null || command.comprobanteId() == null) {
            return Uni.createFrom().failure(new ErrorNegocioException(FirmaError.COMANDO_FIRMA_INVALIDO, command));
        }
        // Tres pasos y ninguna transacción abierta mientras se firma (CPU): una lectura corta del inbox, el certificado
        // (con su propia transacción corta) y, al final, una transacción corta con el resultado y la marca del inbox.
        return Panache.withSession(() -> processedMessageRepository.esMensajeProcesado(command.eventId()))
            .onItem().transformToUni(processed -> {
                if (Boolean.TRUE.equals(processed)) {
                    LogCodificado.info(LOG, CodigoLog.COMANDO_DUPLICADO_DESCARTADO, "comprobanteId=" + command.comprobanteId(),
                        command.eventId());
                    return Uni.createFrom().voidItem();
                }
                return firmarSinTransaccion(command)
                    .onItem().transformToUni(resultado -> Panache.withTransaction(
                        () -> registrarResultado(command, resultado.xmlFirmado() != null, resultado.xmlFirmado(),
                            resultado.motivoError())
                            .chain(() -> processedMessageRepository.persist(ProcessedMessageEntity.of(command.eventId())))))
                    .replaceWithVoid();
            });
    }

    /** El resultado de firmar: el XML firmado, o el motivo del fallo (nunca los dos). */
    private record ResultadoFirma(String xmlFirmado, String motivoError) {
    }

    /** El certificado localizado, o la falla (certificado vencido, empresa sin certificado...). */
    private record Localizacion(CertificadoDigital certificado, Throwable falla) {
    }

    private Uni<ResultadoFirma> firmarSinTransaccion(FirmarComprobanteCommand command) {
        // Si el certificado está vencido, localizarlo lo marca EXPIRADO y falla: la falla se recupera dentro de la
        // transacción para que esa marca se confirme.
        return Panache.withTransaction(() -> firmaXadesService.localizarCertificadoVigente(command.ruc())
                .onItem().transform(certificado -> new Localizacion(certificado, null))
                .onFailure().recoverWithItem(falla -> new Localizacion(null, falla)))
            .onItem().transformToUni(localizacion -> localizacion.falla() != null
                ? Uni.createFrom().item(registrarFalla(command, localizacion.falla()))
                : firmaXadesService.firmar(localizacion.certificado(), command.xmlSinFirmar())
                    .onItem().transform(xmlFirmado -> new ResultadoFirma(xmlFirmado, null))
                    .onFailure().recoverWithItem(falla -> registrarFalla(command, falla)));
    }

    private ResultadoFirma registrarFalla(FirmarComprobanteCommand command, Throwable falla) {
        LogCodificado.error(LOG, falla, CodigoLog.FIRMA_FALLIDA, "comprobanteId=" + command.comprobanteId());
        return new ResultadoFirma(null, falla.getMessage());
    }

    private Uni<Void> registrarResultado(FirmarComprobanteCommand command, boolean exitoso, String xmlFirmado,
            String motivoError) {
        String tipoEvento = exitoso
            ? MessagingConstants.EVENT_TYPE_COMPROBANTE_FIRMADO
            : MessagingConstants.EVENT_TYPE_FIRMA_FALLIDA;
        ComprobanteFirmadoEventMessage mensaje = new ComprobanteFirmadoEventMessage(
            UUID.randomUUID(), command.comprobanteId(), tipoEvento, xmlFirmado, motivoError, Instant.now());
        OutboxEventEntity evento = OutboxEventEntity.of(command.comprobanteId(), tipoEvento,
            MessagingConstants.FIRMA_EVENTS_TOPIC, serializarAJson(mensaje));
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
