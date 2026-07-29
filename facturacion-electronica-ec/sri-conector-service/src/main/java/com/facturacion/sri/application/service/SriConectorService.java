package com.facturacion.sri.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

import com.facturacion.sri.application.dto.EnvioSriResponse;
import com.facturacion.sri.application.event.EnviarComprobanteCommand;
import com.facturacion.sri.application.event.SriResultadoEventMessage;
import com.facturacion.sri.domain.model.entity.ConfiguracionEmpresaLocal;
import com.facturacion.sri.domain.model.entity.EnvioSri;
import com.facturacion.sri.domain.model.enums.AmbienteSri;
import com.facturacion.sri.domain.model.enums.EstadoEnvioSri;
import com.facturacion.sri.domain.repository.ConfiguracionEmpresaLocalRepository;
import com.facturacion.sri.domain.repository.EnvioSriRepository;
import com.facturacion.sri.infrastructure.outbox.OutboxEventEntity;
import com.facturacion.sri.infrastructure.outbox.OutboxEventRepository;
import com.facturacion.sri.infrastructure.persistence.mapper.EnvioSriMapper;
import com.facturacion.sri.infrastructure.soap.AutorizacionResultado;
import com.facturacion.sri.infrastructure.soap.RecepcionResultado;
import com.facturacion.sri.infrastructure.soap.SriAutorizacionClient;
import com.facturacion.sri.infrastructure.soap.SriRecepcionClient;
import com.facturacion.sri.shared.constant.MessagingConstants;
import com.facturacion.sri.shared.exception.NotFoundException;

/**
 * Dueño del agregado EnvioSri. {@link #procesarComando} arranca el flujo
 * (llama a Recepción, ver ficha_tecnica_233.txt sección del WS de Recepción);
 * {@link #consultarAutorizacion} es lo que el poller (@code AutorizacionPoller)
 * llama repetidamente hasta que el SRI devuelva AUTORIZADO/NO AUTORIZADO —
 * el envío inicial a Recepción es inmediato (cumple la Resolución
 * NAC-DGERCGC25-00000017), el polling de Autorización sí usa backoff porque
 * el propio SRI documenta hasta 24h de SLA.
 */
@ApplicationScoped
public class SriConectorService {

    private static final Logger LOG = Logger.getLogger(SriConectorService.class);

    private final EnvioSriRepository envioSriRepository;
    private final SriRecepcionClient recepcionClient;
    private final SriAutorizacionClient autorizacionClient;
    private final ConfiguracionEmpresaLocalRepository configuracionEmpresaLocalRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final EnvioSriMapper mapper;

    @ConfigProperty(name = "sri.autorizacion.poll.max-intentos")
    int maxIntentosAutorizacion;

    @Inject
    public SriConectorService(EnvioSriRepository envioSriRepository, SriRecepcionClient recepcionClient,
            SriAutorizacionClient autorizacionClient,
            ConfiguracionEmpresaLocalRepository configuracionEmpresaLocalRepository,
            OutboxEventRepository outboxRepository, ObjectMapper objectMapper, EnvioSriMapper mapper) {
        this.envioSriRepository = envioSriRepository;
        this.recepcionClient = recepcionClient;
        this.autorizacionClient = autorizacionClient;
        this.configuracionEmpresaLocalRepository = configuracionEmpresaLocalRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.mapper = mapper;
    }

    /**
     * Llama a Recepción (I/O bloqueante en el pool de workers) y solo
     * después toca Hibernate Reactive — hop-back al contexto de Vert.x del
     * llamador, mismo patrón que {@code FirmaXadesService.firmarXml} en
     * firma-electronica-service (ver feedback_quarkus_reactive_kafka_gotchas).
     */
    @WithTransaction
    public Uni<Void> procesarComando(EnviarComprobanteCommand command) {
        io.vertx.core.Context callingContext = Vertx.currentContext();
        AmbienteSri ambiente = AmbienteSri.valueOf(command.ambiente());
        return configuracionEmpresaLocalRepository.findByRucAndClave(command.empresaRuc(), "sri.recepcion.url")
            .onItem().transformToUni(configuracion -> {
                String urlOverride = configuracion == null ? null : configuracion.getValor();
                return recepcionClient.validarComprobante(command.xmlFirmado(), ambiente, urlOverride);
            })
            .emitOn(runnable -> callingContext.runOnContext(ignored -> runnable.run()))
            .onItem().transformToUni(resultado -> guardarResultadoRecepcion(command, ambiente, resultado));
    }

    private Uni<Void> guardarResultadoRecepcion(EnviarComprobanteCommand command, AmbienteSri ambiente,
            RecepcionResultado resultado) {
        Instant ahora = Instant.now();
        EnvioSri envio = new EnvioSri();
        envio.setId(UUID.randomUUID());
        envio.setComprobanteId(command.comprobanteId());
        envio.setClaveAcceso(command.claveAcceso());
        envio.setEmpresaRuc(command.empresaRuc());
        envio.setAmbiente(ambiente);
        envio.setXmlFirmado(command.xmlFirmado());
        envio.setEstado(resultado.recibida() ? EstadoEnvioSri.RECIBIDO : EstadoEnvioSri.RECHAZADO_RECEPCION);
        envio.setMensajesRecepcion(resultado.mensajes());
        envio.setIntentosAutorizacion(0);
        envio.setCreadoEn(ahora);
        envio.setActualizadoEn(ahora);

        String eventType = resultado.recibida() ? MessagingConstants.EVENT_TYPE_SRI_RECIBIDO
            : MessagingConstants.EVENT_TYPE_SRI_RECHAZADO_RECEPCION;
        SriResultadoEventMessage mensaje = new SriResultadoEventMessage(UUID.randomUUID(), command.comprobanteId(),
            command.claveAcceso(), eventType, null, null, null, resultado.mensajes(), ahora);

        LOG.infof("Recepción SRI para %s: %s%s", command.claveAcceso(), envio.getEstado(),
            resultado.mensajes() != null ? " — " + resultado.mensajes() : "");

        return envioSriRepository.persist(envio)
            .chain(guardado -> outboxRepository.persist(OutboxEventEntity.of(command.comprobanteId(), eventType,
                MessagingConstants.SRI_EVENTS_TOPIC, writeValueAsString(mensaje))))
            .replaceWithVoid();
    }

    /**
     * Llamado por el poller para un envío puntual en estado RECIBIDO. La URL y
     * el máximo de intentos pueden venir de un ajuste por empresa
     * ({@code configuracion_empresa} de firma-electronica-service, vía
     * {@code ConfiguracionEmpresaLocal}) — si no hay ajuste, se usan los
     * valores globales de {@code application.properties}.
     */
    @WithTransaction
    public Uni<Void> consultarAutorizacion(EnvioSri envio) {
        io.vertx.core.Context callingContext = Vertx.currentContext();
        Uni<ConfiguracionEmpresaLocal> urlUni = configuracionEmpresaLocalRepository
            .findByRucAndClave(envio.getEmpresaRuc(), "sri.autorizacion.url");
        Uni<ConfiguracionEmpresaLocal> maxIntentosUni = configuracionEmpresaLocalRepository
            .findByRucAndClave(envio.getEmpresaRuc(), "sri.autorizacion.poll.max-intentos");

        return Uni.combine().all().unis(urlUni, maxIntentosUni).asTuple()
            .onItem().transformToUni(tuple -> {
                String urlOverride = tuple.getItem1() == null ? null : tuple.getItem1().getValor();
                int maxIntentos = resolverMaxIntentos(tuple.getItem2());
                return autorizacionClient.consultar(envio.getClaveAcceso(), envio.getAmbiente(), urlOverride)
                    .emitOn(runnable -> callingContext.runOnContext(ignored -> runnable.run()))
                    .onItem().transformToUni(resultado -> aplicarResultadoAutorizacion(envio, resultado, maxIntentos));
            });
    }

    /** {@code null}/no numérico → el default global (documentado en la migración de configuracion_empresa_local). */
    private int resolverMaxIntentos(ConfiguracionEmpresaLocal configuracion) {
        if (configuracion == null || configuracion.getValor() == null) {
            return maxIntentosAutorizacion;
        }
        try {
            return Integer.parseInt(configuracion.getValor().trim());
        } catch (NumberFormatException e) {
            return maxIntentosAutorizacion;
        }
    }

    private Uni<Void> aplicarResultadoAutorizacion(EnvioSri envio, AutorizacionResultado resultado,
            int maxIntentos) {
        envio.setIntentosAutorizacion(envio.getIntentosAutorizacion() + 1);
        envio.setUltimaConsultaEn(Instant.now());

        if (resultado.pendiente()) {
            if (envio.getIntentosAutorizacion() >= maxIntentos) {
                // Se agotaron los intentos sin que el SRI diera una respuesta
                // final — requiere intervención manual (ver EstadoEnvioSri).
                envio.setEstado(EstadoEnvioSri.ERROR_CONSULTA);
                LOG.warnf("Autorización SRI para %s: se agotaron los %d intentos sin respuesta final",
                    envio.getClaveAcceso(), maxIntentos);
            } else {
                LOG.infof("Autorización SRI para %s: pendiente (intento %d de %d)", envio.getClaveAcceso(),
                    envio.getIntentosAutorizacion(), maxIntentos);
            }
            return envioSriRepository.update(envio).replaceWithVoid();
        }

        envio.setEstado(resultado.autorizado() ? EstadoEnvioSri.AUTORIZADO : EstadoEnvioSri.NO_AUTORIZADO);
        envio.setXmlAutorizado(resultado.xmlAutorizacionCompleto());
        envio.setNumeroAutorizacion(resultado.numeroAutorizacion());
        envio.setFechaAutorizacion(resultado.fechaAutorizacion());
        envio.setMotivoRechazo(resultado.motivoRechazo());

        String eventType = resultado.autorizado() ? MessagingConstants.EVENT_TYPE_SRI_AUTORIZADO
            : MessagingConstants.EVENT_TYPE_SRI_NO_AUTORIZADO;
        SriResultadoEventMessage mensaje = new SriResultadoEventMessage(UUID.randomUUID(), envio.getComprobanteId(),
            envio.getClaveAcceso(), eventType, resultado.xmlAutorizacionCompleto(), resultado.numeroAutorizacion(),
            resultado.fechaAutorizacion(), resultado.motivoRechazo(), Instant.now());

        LOG.infof("Autorización SRI para %s: %s", envio.getClaveAcceso(), envio.getEstado());

        return envioSriRepository.update(envio)
            .chain(guardado -> outboxRepository.persist(OutboxEventEntity.of(envio.getComprobanteId(), eventType,
                MessagingConstants.SRI_EVENTS_TOPIC, writeValueAsString(mensaje))))
            .replaceWithVoid();
    }

    @io.quarkus.hibernate.reactive.panache.common.WithSession
    public Uni<List<EnvioSri>> listarPendientesDeAutorizacion(int batchSize) {
        return envioSriRepository.findPendientesDeAutorizacion(EstadoEnvioSri.RECIBIDO, batchSize);
    }

    @io.quarkus.hibernate.reactive.panache.common.WithSession
    public Uni<EnvioSri> obtenerDominioPorComprobanteId(UUID comprobanteId) {
        return envioSriRepository.findByComprobanteId(comprobanteId)
            .onItem().ifNull().failWith(() ->
                new NotFoundException("No hay ningún envío al SRI para el comprobante " + comprobanteId));
    }

    @io.quarkus.hibernate.reactive.panache.common.WithSession
    public Uni<EnvioSriResponse> obtenerPorComprobanteId(UUID comprobanteId) {
        return envioSriRepository.findByComprobanteId(comprobanteId)
            .onItem().ifNull().failWith(() ->
                new NotFoundException("No hay ningún envío al SRI para el comprobante " + comprobanteId))
            .map(mapper::toResponse);
    }

    @io.quarkus.hibernate.reactive.panache.common.WithSession
    public Uni<EnvioSriResponse> obtenerPorClaveAcceso(String claveAcceso) {
        return envioSriRepository.findByClaveAcceso(claveAcceso)
            .onItem().ifNull().failWith(() ->
                new NotFoundException("No hay ningún envío al SRI con clave de acceso " + claveAcceso))
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
