package com.facturacion.notificacion.application.service;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

import com.facturacion.notificacion.application.dto.NotificacionEnviadaResponse;
import com.facturacion.notificacion.application.event.ComprobanteEventMessage;
import com.facturacion.notificacion.application.event.NotificacionResultadoEventMessage;
import com.facturacion.notificacion.domain.model.entity.ConfiguracionEmpresaLocal;
import com.facturacion.notificacion.domain.model.entity.NotificacionEnviada;
import com.facturacion.notificacion.domain.model.enums.EstadoNotificacion;
import com.facturacion.notificacion.domain.repository.ConfiguracionEmpresaLocalRepository;
import com.facturacion.notificacion.domain.repository.NotificacionEnviadaRepository;
import com.facturacion.notificacion.infrastructure.mail.CorreoService;
import com.facturacion.notificacion.infrastructure.outbox.OutboxEventEntity;
import com.facturacion.notificacion.infrastructure.outbox.OutboxEventRepository;
import com.facturacion.notificacion.infrastructure.persistence.mapper.NotificacionEnviadaMapper;
import com.facturacion.notificacion.shared.constant.MessagingConstants;
import com.facturacion.notificacion.shared.exception.NotFoundException;

/**
 * Dueño del agregado NotificacionEnviada. {@link #procesarComprobanteAutorizado}
 * decodifica el RIDE (viene en base64 dentro del evento — este servicio
 * nunca le pide nada a comprobantes-service, cero REST entre servicios),
 * manda el correo real y deja el resultado en el outbox.
 */
@ApplicationScoped
public class NotificacionService {

    private static final Logger LOG = Logger.getLogger(NotificacionService.class);

    private final NotificacionEnviadaRepository notificacionRepository;
    private final CorreoService correoService;
    private final ConfiguracionEmpresaLocalRepository configuracionEmpresaLocalRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final NotificacionEnviadaMapper mapper;

    @Inject
    public NotificacionService(NotificacionEnviadaRepository notificacionRepository, CorreoService correoService,
            ConfiguracionEmpresaLocalRepository configuracionEmpresaLocalRepository,
            OutboxEventRepository outboxRepository, ObjectMapper objectMapper, NotificacionEnviadaMapper mapper) {
        this.notificacionRepository = notificacionRepository;
        this.correoService = correoService;
        this.configuracionEmpresaLocalRepository = configuracionEmpresaLocalRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.mapper = mapper;
    }

    /**
     * El envío de correo es I/O bloqueante (JavaMail por debajo) — se
     * corre en el pool de workers de Mutiny y se vuelve al contexto de
     * Vert.x del llamador antes de tocar Hibernate Reactive, mismo patrón
     * que {@code FirmaXadesService}/los clientes SOAP de sri-conector-service.
     */
    @WithTransaction
    public Uni<Void> procesarComprobanteAutorizado(ComprobanteEventMessage evento) {
        io.vertx.core.Context callingContext = Vertx.currentContext();
        byte[] ridePdf = Base64.getDecoder().decode(evento.ridePdfBase64());

        // Ajustes opcionales por empresa (configuracion_empresa en
        // firma-electronica-service, vía ConfiguracionEmpresaLocal) — null si
        // nunca se configuraron, CorreoService usa su comportamiento por
        // defecto en ese caso.
        Uni<ConfiguracionEmpresaLocal> asuntoUni =
            configuracionEmpresaLocalRepository.findByRucAndClave(evento.empresaRuc(), "notificacion.asunto");
        Uni<ConfiguracionEmpresaLocal> remitenteUni = configuracionEmpresaLocalRepository
            .findByRucAndClave(evento.empresaRuc(), "notificacion.remitente_nombre");

        return Uni.combine().all().unis(asuntoUni, remitenteUni).asTuple()
            .onItem().transformToUni(tuple -> {
                String asunto = tuple.getItem1() == null ? null : tuple.getItem1().getValor();
                String remitenteNombre = tuple.getItem2() == null ? null : tuple.getItem2().getValor();
                return correoService.enviarComprobante(evento.compradorCorreo(), evento.claveAcceso(),
                        evento.tipoComprobante(), ridePdf, evento.xmlAutorizado(), asunto, remitenteNombre)
                    .onItem().transform(ignorado -> (String) null)
                    .onFailure().recoverWithItem(falla -> {
                        LOG.errorf(falla, "No se pudo enviar el correo del comprobante %s a %s", evento.claveAcceso(),
                            evento.compradorCorreo());
                        return falla.getMessage();
                    });
            })
            .emitOn(runnable -> callingContext.runOnContext(ignored -> runnable.run()))
            .onItem().transformToUni(motivoError -> guardarResultado(evento, motivoError));
    }

    private Uni<Void> guardarResultado(ComprobanteEventMessage evento, String motivoError) {
        Instant ahora = Instant.now();
        NotificacionEnviada notificacion = new NotificacionEnviada();
        notificacion.setId(UUID.randomUUID());
        notificacion.setComprobanteId(evento.comprobanteId());
        notificacion.setClaveAcceso(evento.claveAcceso());
        notificacion.setDestinatario(evento.compradorCorreo());
        notificacion.setEstado(motivoError == null ? EstadoNotificacion.ENVIADA : EstadoNotificacion.FALLIDA);
        notificacion.setMotivoError(motivoError);
        notificacion.setEnviadoEn(ahora);

        String eventType = motivoError == null ? MessagingConstants.EVENT_TYPE_NOTIFICACION_ENVIADA
            : MessagingConstants.EVENT_TYPE_NOTIFICACION_FALLIDA;
        NotificacionResultadoEventMessage mensaje = new NotificacionResultadoEventMessage(UUID.randomUUID(),
            evento.comprobanteId(), eventType, evento.compradorCorreo(), motivoError, ahora);

        return notificacionRepository.persist(notificacion)
            .chain(guardado -> outboxRepository.persist(OutboxEventEntity.of(evento.comprobanteId(), eventType,
                MessagingConstants.NOTIFICACION_EVENTS_TOPIC, writeValueAsString(mensaje))))
            .replaceWithVoid();
    }

    @WithSession
    public Uni<NotificacionEnviadaResponse> obtenerPorComprobanteId(UUID comprobanteId) {
        return notificacionRepository.findByComprobanteId(comprobanteId)
            .onItem().ifNull().failWith(() ->
                new NotFoundException("No hay ninguna notificación para el comprobante " + comprobanteId))
            .map(mapper::toResponse);
    }

    private String writeValueAsString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el payload del outbox", e);
        }
    }
}
