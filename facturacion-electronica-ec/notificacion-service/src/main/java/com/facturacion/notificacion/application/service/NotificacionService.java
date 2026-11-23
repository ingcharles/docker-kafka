package com.facturacion.notificacion.application.service;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.facturacion.commons.error.ErrorComun;
import com.facturacion.commons.error.LogCodificado;
import com.facturacion.commons.exception.ErrorInternoException;
import com.facturacion.commons.exception.NotFoundException;
import com.facturacion.notificacion.application.dto.NotificacionEnviadaResponse;
import com.facturacion.notificacion.application.event.ComprobanteEventMessage;
import com.facturacion.notificacion.application.event.NotificacionResultadoEventMessage;
import com.facturacion.notificacion.application.mapper.NotificacionEnviadaResponseMapper;
import com.facturacion.notificacion.domain.model.entity.NotificacionEnviada;
import com.facturacion.notificacion.domain.model.enums.EstadoNotificacion;
import com.facturacion.notificacion.domain.model.enums.TipoComprobanteCorreo;
import com.facturacion.notificacion.domain.port.CorreoSalida;
import com.facturacion.notificacion.domain.port.EventoSaliente;
import com.facturacion.notificacion.domain.port.EventosSalientes;
import com.facturacion.notificacion.domain.repository.NotificacionEnviadaRepository;
import com.facturacion.notificacion.domain.service.ClaveAcceso;
import com.facturacion.notificacion.domain.service.MotivoFalla;
import com.facturacion.notificacion.shared.constant.MessagingConstants;
import com.facturacion.notificacion.shared.error.CodigoLog;
import com.facturacion.notificacion.shared.error.NotificacionError;

/**
 * Dueño del agregado NotificacionEnviada. {@link #enviarCorreo}
 * decodifica el RIDE (viene en base64 dentro del evento — este servicio
 * nunca le pide nada a comprobantes-service, cero REST entre servicios),
 * manda el correo real y deja el resultado en el outbox.
 */
@ApplicationScoped
public class NotificacionService {

    private static final Logger LOG = Logger.getLogger(NotificacionService.class);

    private final NotificacionEnviadaRepository notificacionRepository;
    private final CorreoSalida correoService;
    private final LectorAjustesCorreo lectorAjustes;
    private final EventosSalientes salientes;
    private final ObjectMapper objectMapper;
    private final NotificacionEnviadaResponseMapper mapper;

    @Inject
    public NotificacionService(NotificacionEnviadaRepository notificacionRepository, CorreoSalida correoService,
            LectorAjustesCorreo lectorAjustes,
            EventosSalientes salientes, ObjectMapper objectMapper, NotificacionEnviadaResponseMapper mapper) {
        this.notificacionRepository = notificacionRepository;
        this.correoService = correoService;
        this.lectorAjustes = lectorAjustes;
        this.salientes = salientes;
        this.objectMapper = objectMapper;
        this.mapper = mapper;
    }

    /**
     * Envía el correo y devuelve el motivo del fallo ({@code null} si salió bien). No hay transacción ni sesión de base
     * de datos abierta durante el envío: el SMTP puede tardar segundos (y el limitador de correo hace esperar su turno),
     * y con una transacción abierta cada correo en vuelo retendría una conexión del pool. Los ajustes de la empresa se
     * leen en una sesión corta que se cierra antes de enviar; el resultado se guarda después con
     * {@link #guardarResultado}.
     */
    public Uni<String> enviarCorreo(ComprobanteEventMessage evento) {
        Context callingContext = Vertx.currentContext();
        byte[] ridePdf = Base64.getDecoder().decode(evento.ridePdfBase64());
        TipoComprobanteCorreo tipo = TipoComprobanteCorreo.obtenerPorNombre(evento.tipoComprobante());
        return Panache.withSession(() -> lectorAjustes.leer(evento.empresaRuc(), tipo, obtenerNombreDeLaEmpresa(evento)))
            .onItem().transformToUni(ajustes -> correoService.enviarComprobante(new CorreoSalida.Comprobante(
                    evento.compradorCorreo(), evento.claveAcceso(), evento.tipoComprobante(),
                    evento.compradorRazonSocial(), evento.importeTotal(), ridePdf, evento.xmlAutorizado(), ajustes))
                .onItem().transform(ignorado -> (String) null)
                .onFailure().recoverWithItem(falla -> {
                    LogCodificado.error(LOG, falla, CodigoLog.CORREO_FALLIDO, "claveAcceso=" + evento.claveAcceso());
                    return MotivoFalla.obtenerMotivo(falla);
                }))
            .emitOn(runnable -> callingContext.runOnContext(ignored -> runnable.run()));
    }

    /** El nombre de la empresa para el texto del correo: su nombre comercial o, si no tiene, su razón social. */
    private static String obtenerNombreDeLaEmpresa(ComprobanteEventMessage evento) {
        String comercial = evento.empresaNombreComercial();
        return comercial != null && !comercial.isBlank() ? comercial : evento.empresaRazonSocial();
    }

    /**
     * Un comprobante tiene una sola notificacion (índice único): el primer envío la crea y un reenvío (evento nuevo
     * del mismo comprobante) actualiza su resultado.
     */
    @WithTransaction
    public Uni<Void> guardarResultado(ComprobanteEventMessage evento, String motivoError) {
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
                && empresaRuc.equals(ClaveAcceso.obtenerRucDelEmisor(n.getClaveAcceso())) ? n : null)
            .onItem().ifNull().failWith(() ->
                new NotFoundException(NotificacionError.NOTIFICACION_NO_ENCONTRADA, comprobanteId))
            .map(mapper::toResponse);
    }

    private String serializarAJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new ErrorInternoException(e, ErrorComun.PAYLOAD_NO_SERIALIZABLE);
        }
    }
}
