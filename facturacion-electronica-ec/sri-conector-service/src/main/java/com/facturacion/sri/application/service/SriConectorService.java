package com.facturacion.sri.application.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.facturacion.commons.error.ErrorComun;
import com.facturacion.commons.error.LogCodificado;
import com.facturacion.commons.exception.ErrorInternoException;
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
import com.facturacion.sri.shared.error.SriError;

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

    @ConfigProperty(name = "sri.autorizacion.poll.reserva")
    Duration reservaConsulta;

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
     * llamar al SRI sin BD, y guardar el resultado en una transacción corta (el cliente del SRI usa
     * {@code TrabajoBloqueante}, que vuelve al contexto de Vert.x antes de tocar Hibernate Reactive).
     * <p>
     * Si el guardado falla tras haber enviado, el comando se reprocesa y el SRI contesta que la clave de acceso ya
     * fue recibida; ese caso ya se trata como idempotente por la clave de acceso.
     */
    public Uni<Void> procesarComando(EnviarComprobanteCommand command) {
        AmbienteSri ambiente = AmbienteSri.valueOf(command.ambiente());
        return Panache.withSession(
                () -> configuracionEmpresaLocalRepository.findByRucAndClave(command.empresaRuc(), "sri.recepcion.url"))
            .onItem().transformToUni(configuracion -> {
                String urlOverride = configuracion == null ? null : configuracion.getValor();
                return recepcionClient.validarComprobante(command.xmlFirmado(), ambiente, urlOverride);
            })
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
        envio.setEstado(resultado.esRecibida() ? EstadoEnvioSri.RECIBIDO : EstadoEnvioSri.RECHAZADO_RECEPCION);
        envio.setMensajesRecepcion(resultado.mensajes());
        envio.setIntentosAutorizacion(0);
        envio.setCreadoEn(ahora);
        envio.setActualizadoEn(ahora);

        String eventType = resultado.esRecibida() ? MessagingConstants.EVENT_TYPE_SRI_RECIBIDO
            : MessagingConstants.EVENT_TYPE_SRI_RECHAZADO_RECEPCION;
        SriResultadoEventMessage mensaje = new SriResultadoEventMessage(UUID.randomUUID(), command.comprobanteId(),
            command.claveAcceso(), eventType, null, null, null, resultado.mensajes(), ahora);

        if (resultado.esRecibida()) {
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
                .onItem().transformToUni(resultado -> Panache.withTransaction(
                    () -> aplicarSiSigueVigente(envio, resultado, ajustes.maxIntentos()))));
    }

    /**
     * Con 2 o más instancias, otra pudo consultar el mismo envío mientras esta esperaba al SRI (la reserva dura poco). Se
     * toma el envío con bloqueo y solo se aplica el resultado si nadie más lo aplicó; si no, se descarta y no se publica
     * un segundo evento.
     */
    private Uni<Void> aplicarSiSigueVigente(EnvioSri envio, AutorizacionResultado resultado, int maxIntentos) {
        return envioSriRepository.findByIdBloqueado(envio.getId())
            .onItem().transformToUni(actual -> {
                if (actual == null || !actual.esConsultaVigente(envio.getIntentosAutorizacion())) {
                    LogCodificado.info(LOG, CodigoLog.CONSULTA_DESCARTADA, "claveAcceso=" + envio.getClaveAcceso());
                    return Uni.createFrom().voidItem();
                }
                return aplicarResultadoAutorizacion(envio, resultado, maxIntentos);
            });
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

        if (resultado.esPendiente()) {
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

        envio.setEstado(resultado.esAutorizado() ? EstadoEnvioSri.AUTORIZADO : EstadoEnvioSri.NO_AUTORIZADO);
        envio.setXmlAutorizado(resultado.xmlAutorizacionCompleto());
        envio.setNumeroAutorizacion(resultado.numeroAutorizacion());
        envio.setFechaAutorizacion(resultado.fechaAutorizacion());
        envio.setMotivoRechazo(resultado.motivoRechazo());

        String eventType = resultado.esAutorizado() ? MessagingConstants.EVENT_TYPE_SRI_AUTORIZADO
            : MessagingConstants.EVENT_TYPE_SRI_NO_AUTORIZADO;
        SriResultadoEventMessage mensaje = new SriResultadoEventMessage(UUID.randomUUID(), envio.getComprobanteId(),
            envio.getClaveAcceso(), eventType, resultado.xmlAutorizacionCompleto(), resultado.numeroAutorizacion(),
            resultado.fechaAutorizacion(), resultado.motivoRechazo(), Instant.now());

        if (resultado.esAutorizado()) {
            LogCodificado.info(LOG, CodigoLog.AUTORIZACION_RESULTADO, "claveAcceso=" + envio.getClaveAcceso(), envio.getEstado());
        } else {
            LogCodificado.error(LOG, CodigoLog.SRI_NO_AUTORIZA, "claveAcceso=" + envio.getClaveAcceso(), resultado.motivoRechazo());
        }

        return envioSriRepository.update(envio)
            .chain(guardado -> salientes.registrar(new EventoSaliente(envio.getComprobanteId(), eventType,
                MessagingConstants.SRI_EVENTS_TOPIC, serializarAJson(mensaje))))
            .replaceWithVoid();
    }

    /**
     * Toma los envíos que toca consultar y los reserva en una transacción corta (con {@code SKIP LOCKED}), para que otra
     * instancia no consulte los mismos al SRI. La consulta en sí va después, sin transacción abierta.
     */
    @WithTransaction
    public Uni<List<EnvioSri>> reclamarPendientesDeAutorizacion(int batchSize) {
        Instant ahora = Instant.now();
        return envioSriRepository.reclamarPendientesDeAutorizacion(batchSize, ahora.minus(reservaConsulta), ahora);
    }

    @WithSession
    public Uni<EnvioSri> obtenerDominioPorComprobanteId(UUID comprobanteId, String empresaRuc) {
        return envioSriRepository.findByComprobanteId(comprobanteId)
            .onItem().transform(envio -> filtrarPorEmpresa(envio, empresaRuc))
            .onItem().ifNull().failWith(() ->
                new NotFoundException(SriError.ENVIO_NO_ENCONTRADO_POR_COMPROBANTE, comprobanteId));
    }

    @WithSession
    public Uni<EnvioSriResponse> obtenerPorComprobanteId(UUID comprobanteId, String empresaRuc) {
        return envioSriRepository.findByComprobanteId(comprobanteId)
            .onItem().transform(envio -> filtrarPorEmpresa(envio, empresaRuc))
            .onItem().ifNull().failWith(() ->
                new NotFoundException(SriError.ENVIO_NO_ENCONTRADO_POR_COMPROBANTE, comprobanteId))
            .map(mapper::toResponse);
    }

    @WithSession
    public Uni<EnvioSriResponse> obtenerPorClaveAcceso(String claveAcceso, String empresaRuc) {
        return envioSriRepository.findByClaveAcceso(claveAcceso)
            .onItem().transform(envio -> filtrarPorEmpresa(envio, empresaRuc))
            .onItem().ifNull().failWith(() ->
                new NotFoundException(SriError.ENVIO_NO_ENCONTRADO_POR_CLAVE, claveAcceso))
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
            throw new ErrorInternoException(e, ErrorComun.PAYLOAD_NO_SERIALIZABLE);
        }
    }
}
