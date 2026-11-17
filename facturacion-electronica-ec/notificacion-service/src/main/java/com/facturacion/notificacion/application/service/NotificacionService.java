package com.facturacion.notificacion.application.service;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.facturacion.commons.error.LogCodificado;
import com.facturacion.commons.exception.NotFoundException;
import com.facturacion.notificacion.application.dto.NotificacionEnviadaResponse;
import com.facturacion.notificacion.application.event.ComprobanteEventMessage;
import com.facturacion.notificacion.application.event.NotificacionResultadoEventMessage;
import com.facturacion.notificacion.application.mapper.NotificacionEnviadaResponseMapper;
import com.facturacion.notificacion.domain.model.entity.ConfiguracionEmpresaLocal;
import com.facturacion.notificacion.domain.model.entity.NotificacionEnviada;
import com.facturacion.notificacion.domain.model.enums.EstadoNotificacion;
import com.facturacion.notificacion.domain.port.CorreoSalida;
import com.facturacion.notificacion.domain.port.EventoSaliente;
import com.facturacion.notificacion.domain.port.EventosSalientes;
import com.facturacion.notificacion.domain.repository.ConfiguracionEmpresaLocalRepository;
import com.facturacion.notificacion.domain.repository.NotificacionEnviadaRepository;
import com.facturacion.notificacion.domain.service.ClaveAcceso;
import com.facturacion.notificacion.domain.service.MotivoFalla;
import com.facturacion.notificacion.shared.constant.MessagingConstants;
import com.facturacion.notificacion.shared.error.CodigoLog;

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
    private final CorreoSalida correoService;
    private final ConfiguracionEmpresaLocalRepository configuracionEmpresaLocalRepository;
    private final EventosSalientes salientes;
    private final ObjectMapper objectMapper;
    private final NotificacionEnviadaResponseMapper mapper;

    @Inject
    public NotificacionService(NotificacionEnviadaRepository notificacionRepository, CorreoSalida correoService,
            ConfiguracionEmpresaLocalRepository configuracionEmpresaLocalRepository,
            EventosSalientes salientes, ObjectMapper objectMapper, NotificacionEnviadaResponseMapper mapper) {
        this.notificacionRepository = notificacionRepository;
        this.correoService = correoService;
        this.configuracionEmpresaLocalRepository = configuracionEmpresaLocalRepository;
        this.salientes = salientes;
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
        // nunca se configuraron, CorreoSalida usa su comportamiento por
        // defecto en ese caso. Secuencial a propósito (no Uni.combine): dos
        // consultas Hibernate Reactive concurrentes sobre la misma sesión
        // reactiva rompen el procesamiento del result set ("Illegal pop()
        // with non-matching JdbcValuesSourceProcessingState") — mismo bug ya
        // encontrado y arreglado en ComprobanteService.emitir/SriConectorService.consultarAutorizacion.
        return configuracionEmpresaLocalRepository.findByRucAndClave(evento.empresaRuc(), "notificacion.asunto")
            .onItem().transformToUni(configuracionAsunto -> configuracionEmpresaLocalRepository
                .findByRucAndClave(evento.empresaRuc(), "notificacion.remitente_nombre")
                .onItem().transformToUni(configuracionRemitente -> {
                    String asunto = configuracionAsunto == null ? null : configuracionAsunto.getValor();
                    String remitenteNombre = configuracionRemitente == null ? null : configuracionRemitente.getValor();
                    return correoService.enviarComprobante(evento.compradorCorreo(), evento.claveAcceso(),
                            evento.tipoComprobante(), ridePdf, evento.xmlAutorizado(), asunto, remitenteNombre)
                        .onItem().transform(ignorado -> (String) null)
                        .onFailure().recoverWithItem(falla -> {
                            LogCodificado.error(LOG, falla, CodigoLog.CORREO_FALLIDO, "claveAcceso=" + evento.claveAcceso());
                            return MotivoFalla.de(falla);
                        });
                }))
            .emitOn(runnable -> callingContext.runOnContext(ignored -> runnable.run()))
            .onItem().transformToUni(motivoError -> guardarResultado(evento, motivoError));
    }

    /**
     * Un comprobante tiene una sola notificacion (índice único): el primer envío la crea y un reenvío (evento nuevo
     * del mismo comprobante) actualiza su resultado.
     */
    private Uni<Void> guardarResultado(ComprobanteEventMessage evento, String motivoError) {
        Instant ahora = Instant.now();
        return notificacionRepository.findByComprobanteId(evento.comprobanteId())
            .onItem().transformToUni(existente -> {
                NotificacionEnviada notificacion = existente != null ? existente : new NotificacionEnviada();
                if (existente == null) {
                    notificacion.setId(UUID.randomUUID());
                    notificacion.setComprobanteId(evento.comprobanteId());
                    notificacion.setClaveAcceso(evento.claveAcceso());
                }
                notificacion.setDestinatario(evento.compradorCorreo());
                notificacion.setEstado(motivoError == null ? EstadoNotificacion.ENVIADA : EstadoNotificacion.FALLIDA);
                notificacion.setMotivoError(motivoError);
                notificacion.setEnviadoEn(ahora);
                Uni<NotificacionEnviada> guardada = existente != null ? notificacionRepository.actualizar(notificacion)
                    : notificacionRepository.persist(notificacion);
                return guardada.chain(() -> registrarResultado(evento, motivoError, ahora));
            });
    }

    private Uni<Void> registrarResultado(ComprobanteEventMessage evento, String motivoError, Instant ahora) {
        String eventType = motivoError == null ? MessagingConstants.EVENT_TYPE_NOTIFICACION_ENVIADA
            : MessagingConstants.EVENT_TYPE_NOTIFICACION_FALLIDA;
        NotificacionResultadoEventMessage mensaje = new NotificacionResultadoEventMessage(UUID.randomUUID(),
            evento.comprobanteId(), eventType, evento.compradorCorreo(), motivoError, ahora);

        return salientes.registrar(new EventoSaliente(evento.comprobanteId(), eventType,
                MessagingConstants.NOTIFICACION_EVENTS_TOPIC, serializarAJson(mensaje)))
            .replaceWithVoid();
    }

    @WithSession
    public Uni<NotificacionEnviadaResponse> obtenerPorComprobanteId(UUID comprobanteId, String empresaRuc) {
        return notificacionRepository.findByComprobanteId(comprobanteId)
            // Una notificación de otra empresa se trata como inexistente (404): no se revela que existe.
            .onItem().transform(n -> n != null && empresaRuc != null
                && empresaRuc.equals(ClaveAcceso.rucDelEmisor(n.getClaveAcceso())) ? n : null)
            .onItem().ifNull().failWith(() ->
                new NotFoundException("No hay ninguna notificación para el comprobante " + comprobanteId))
            .map(mapper::toResponse);
    }

    private String serializarAJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el payload del outbox", e);
        }
    }
}
