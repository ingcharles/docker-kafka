package com.facturacion.sri.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.facturacion.commons.error.LogCodificado;
import com.facturacion.commons.exception.NotFoundException;
import com.facturacion.sri.application.dto.EnvioSriResponse;
import com.facturacion.sri.application.event.EnviarComprobanteCommand;
import com.facturacion.sri.application.event.SriResultadoEventMessage;
import com.facturacion.sri.application.mapper.EnvioSriResponseMapper;
import com.facturacion.sri.domain.model.entity.ConfiguracionEmpresaLocal;
import com.facturacion.sri.domain.model.entity.EnvioSri;
import com.facturacion.sri.domain.model.enums.AmbienteSri;
import com.facturacion.sri.domain.model.enums.EstadoEnvioSri;
import com.facturacion.sri.domain.port.AutorizacionResultado;
import com.facturacion.sri.domain.port.AutorizacionSri;
import com.facturacion.sri.domain.port.EventoSaliente;
import com.facturacion.sri.domain.port.EventosSalientes;
import com.facturacion.sri.domain.port.RecepcionResultado;
import com.facturacion.sri.domain.port.RecepcionSri;
import com.facturacion.sri.domain.repository.ConfiguracionEmpresaLocalRepository;
import com.facturacion.sri.domain.repository.EnvioSriRepository;
import com.facturacion.sri.shared.constant.MessagingConstants;
import com.facturacion.sri.shared.error.CodigoLog;

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
    private final RecepcionSri recepcionClient;
    private final AutorizacionSri autorizacionClient;
    private final ConfiguracionEmpresaLocalRepository configuracionEmpresaLocalRepository;
    private final EventosSalientes salientes;
    private final ObjectMapper objectMapper;
    private final EnvioSriResponseMapper mapper;

    @ConfigProperty(name = "sri.autorizacion.poll.max-intentos")
    int maxIntentosAutorizacion;

    @Inject
    public SriConectorService(EnvioSriRepository envioSriRepository, RecepcionSri recepcionClient,
            AutorizacionSri autorizacionClient,
            ConfiguracionEmpresaLocalRepository configuracionEmpresaLocalRepository,
            EventosSalientes salientes, ObjectMapper objectMapper, EnvioSriResponseMapper mapper) {
        this.envioSriRepository = envioSriRepository;
        this.recepcionClient = recepcionClient;
        this.autorizacionClient = autorizacionClient;
        this.configuracionEmpresaLocalRepository = configuracionEmpresaLocalRepository;
        this.salientes = salientes;
        this.objectMapper = objectMapper;
        this.mapper = mapper;
    }

    /**
     * Llama a Recepción (I/O bloqueante en el pool de workers) SIN sesión ni transacción de BD abiertas: el SRI puede
     * tardar decenas de segundos y, con la transacción abierta, cada comando en vuelo retendría una conexión del pool
     * (unas pocas llamadas lentas lo agotarían). Tres pasos: leer la configuración de la empresa en una sesión corta,
     * llamar al SRI sin BD, y guardar el resultado en una transacción corta (hop-back al contexto de Vert.x del
     * llamador antes de volver a tocar Hibernate Reactive, mismo patrón que {@code FirmaXadesService.firmarXml}).
     * <p>
     * Si el guardado falla tras haber enviado, el comando se reprocesa y el SRI contesta que la clave de acceso ya
     * fue recibida; ese caso ya se trata como idempotente por la clave de acceso.
     */
    public Uni<Void> procesarComando(EnviarComprobanteCommand command) {
        io.vertx.core.Context callingContext = Vertx.currentContext();
        AmbienteSri ambiente = AmbienteSri.valueOf(command.ambiente());
        return Panache.withSession(
                () -> configuracionEmpresaLocalRepository.findByRucAndClave(command.empresaRuc(), "sri.recepcion.url"))
            .onItem().transformToUni(configuracion -> {
                String urlOverride = configuracion == null ? null : configuracion.getValor();
                return recepcionClient.validarComprobante(command.xmlFirmado(), ambiente, urlOverride);
            })
            .emitOn(runnable -> callingContext.runOnContext(ignored -> runnable.run()))
            .onItem().transformToUni(resultado -> Panache.withTransaction(
                () -> guardarResultadoRecepcion(command, ambiente, resultado)));
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

        if (resultado.recibida()) {
            LogCodificado.info(LOG, CodigoLog.RECEPCION_RESULTADO, "claveAcceso=" + command.claveAcceso(), envio.getEstado());
        } else {
            LogCodificado.error(LOG, CodigoLog.SRI_RECHAZA_RECEPCION, "claveAcceso=" + command.claveAcceso(), resultado.mensajes());
        }

        return envioSriRepository.persist(envio)
            .chain(guardado -> salientes.registrar(new EventoSaliente(command.comprobanteId(), eventType,
                MessagingConstants.SRI_EVENTS_TOPIC, serializarAJson(mensaje))))
            .replaceWithVoid();
    }

    /**
     * Llamado por el poller para un envío puntual en estado RECIBIDO. La URL y
     * el máximo de intentos pueden venir de un ajuste por empresa
     * ({@code configuracion_empresa} de firma-electronica-service, vía
     * {@code ConfiguracionEmpresaLocal}) — si no hay ajuste, se usan los
     * valores globales de {@code application.properties}.
     */
    public Uni<Void> consultarAutorizacion(EnvioSri envio) {
        io.vertx.core.Context callingContext = Vertx.currentContext();
        // Mismo esquema que procesarComando: la consulta al SRI no se hace con la transacción abierta. Las dos lecturas
        // de configuración van encadenadas dentro de una sola sesión, a propósito (no Uni.combine): dos consultas
        // Hibernate Reactive concurrentes sobre la misma sesión rompen el procesamiento del result set ("Illegal pop()
        // with non-matching JdbcValuesSourceProcessingState"), mismo bug ya corregido en ComprobanteService.emitir.
        return Panache.withSession(() -> configuracionEmpresaLocalRepository
                .findByRucAndClave(envio.getEmpresaRuc(), "sri.autorizacion.url")
                .onItem().transformToUni(configuracionUrl -> configuracionEmpresaLocalRepository
                    .findByRucAndClave(envio.getEmpresaRuc(), "sri.autorizacion.poll.max-intentos")
                    .map(configuracionMaxIntentos -> new AjustesConsulta(
                        configuracionUrl == null ? null : configuracionUrl.getValor(),
                        resolverMaxIntentos(configuracionMaxIntentos)))))
            .onItem().transformToUni(ajustes -> autorizacionClient
                .consultar(envio.getClaveAcceso(), envio.getAmbiente(), ajustes.urlOverride())
                .emitOn(runnable -> callingContext.runOnContext(ignored -> runnable.run()))
                .onItem().transformToUni(resultado -> Panache.withTransaction(
                    () -> aplicarResultadoAutorizacion(envio, resultado, ajustes.maxIntentos()))));
    }

    private record AjustesConsulta(String urlOverride, int maxIntentos) {
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
                LogCodificado.error(LOG, CodigoLog.SRI_SIN_RESPUESTA_FINAL, "claveAcceso=" + envio.getClaveAcceso(), maxIntentos);
            } else {
                LogCodificado.info(LOG, CodigoLog.AUTORIZACION_PENDIENTE, "claveAcceso=" + envio.getClaveAcceso(),
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

        if (resultado.autorizado()) {
            LogCodificado.info(LOG, CodigoLog.AUTORIZACION_RESULTADO, "claveAcceso=" + envio.getClaveAcceso(), envio.getEstado());
        } else {
            LogCodificado.error(LOG, CodigoLog.SRI_NO_AUTORIZA, "claveAcceso=" + envio.getClaveAcceso(), resultado.motivoRechazo());
        }

        return envioSriRepository.update(envio)
            .chain(guardado -> salientes.registrar(new EventoSaliente(envio.getComprobanteId(), eventType,
                MessagingConstants.SRI_EVENTS_TOPIC, serializarAJson(mensaje))))
            .replaceWithVoid();
    }

    @io.quarkus.hibernate.reactive.panache.common.WithSession
    public Uni<List<EnvioSri>> listarPendientesDeAutorizacion(int batchSize) {
        return envioSriRepository.findPendientesDeAutorizacion(EstadoEnvioSri.RECIBIDO, batchSize);
    }

    @io.quarkus.hibernate.reactive.panache.common.WithSession
    public Uni<EnvioSri> obtenerDominioPorComprobanteId(UUID comprobanteId, String empresaRuc) {
        return envioSriRepository.findByComprobanteId(comprobanteId)
            .onItem().transform(envio -> filtrarPorEmpresa(envio, empresaRuc))
            .onItem().ifNull().failWith(() ->
                new NotFoundException("No hay ningún envío al SRI para el comprobante " + comprobanteId));
    }

    @io.quarkus.hibernate.reactive.panache.common.WithSession
    public Uni<EnvioSriResponse> obtenerPorComprobanteId(UUID comprobanteId, String empresaRuc) {
        return envioSriRepository.findByComprobanteId(comprobanteId)
            .onItem().transform(envio -> filtrarPorEmpresa(envio, empresaRuc))
            .onItem().ifNull().failWith(() ->
                new NotFoundException("No hay ningún envío al SRI para el comprobante " + comprobanteId))
            .map(mapper::toResponse);
    }

    @io.quarkus.hibernate.reactive.panache.common.WithSession
    public Uni<EnvioSriResponse> obtenerPorClaveAcceso(String claveAcceso, String empresaRuc) {
        return envioSriRepository.findByClaveAcceso(claveAcceso)
            .onItem().transform(envio -> filtrarPorEmpresa(envio, empresaRuc))
            .onItem().ifNull().failWith(() ->
                new NotFoundException("No hay ningún envío al SRI con clave de acceso " + claveAcceso))
            .map(mapper::toResponse);
    }

    /**
     * Un envío de otra empresa se trata como inexistente (404): la api-key solo ve lo de su propio RUC y no se
     * revela que el comprobante existe.
     */
    private static EnvioSri filtrarPorEmpresa(EnvioSri envio, String empresaRuc) {
        // empresa_ruc es nullable (envíos anteriores a V2__ruc_envio_sri): Objects.equals evita el NPE → 404.
        return envio != null && Objects.equals(envio.getEmpresaRuc(), empresaRuc) ? envio : null;
    }

    private String serializarAJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el payload del outbox", e);
        }
    }
}
