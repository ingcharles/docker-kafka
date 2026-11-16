package com.facturacion.firma.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.facturacion.commons.exception.NotFoundException;
import com.facturacion.firma.application.dto.ConfiguracionEmpresaResponse;
import com.facturacion.firma.application.event.ConfiguracionEmpresaEventMessage;
import com.facturacion.firma.application.mapper.ConfiguracionEmpresaResponseMapper;
import com.facturacion.firma.domain.model.entity.ConfiguracionEmpresa;
import com.facturacion.firma.domain.model.entity.Empresa;
import com.facturacion.firma.domain.port.EventoSaliente;
import com.facturacion.firma.domain.port.EventosSalientes;
import com.facturacion.firma.domain.repository.ConfiguracionEmpresaRepository;
import com.facturacion.firma.domain.repository.EmpresaRepository;
import com.facturacion.firma.shared.constant.MessagingConstants;
import com.facturacion.firma.shared.exception.CodigoDuplicadoException;

/**
 * Ajustes opcionales por empresa, guardados como pares clave/valor — ver la
 * migración V3 para el razonamiento de por qué esto es una tabla genérica
 * separada en vez de columnas nuevas en {@code Empresa} cada vez que hace
 * falta un ajuste más. Cada {@code establecer}/{@code eliminar} publica un
 * evento en el outbox (mismo patrón que {@code EmpresaService}) para que
 * otros servicios (hoy solo comprobantes-service, para {@code ride.color})
 * mantengan su propia réplica de solo lectura.
 */
@ApplicationScoped
public class ConfiguracionEmpresaService {

    private final EmpresaRepository empresaRepository;
    private final ConfiguracionEmpresaRepository configuracionRepository;
    private final ConfiguracionEmpresaResponseMapper mapper;
    private final EventosSalientes salientes;
    private final ObjectMapper objectMapper;

    @Inject
    public ConfiguracionEmpresaService(EmpresaRepository empresaRepository,
            ConfiguracionEmpresaRepository configuracionRepository, ConfiguracionEmpresaResponseMapper mapper,
            EventosSalientes salientes, ObjectMapper objectMapper) {
        this.empresaRepository = empresaRepository;
        this.configuracionRepository = configuracionRepository;
        this.mapper = mapper;
        this.salientes = salientes;
        this.objectMapper = objectMapper;
    }

    /** POST — crea la clave por primera vez; 409 si ya existía para esta empresa (ver {@link #actualizar} para cambiarla). */
    @WithTransaction
    public Uni<ConfiguracionEmpresaResponse> crear(UUID empresaId, String clave, String valor) {
        return empresaRepository.findById(empresaId)
            .onItem().ifNull().failWith(() -> new NotFoundException("Empresa no encontrada: " + empresaId))
            .onItem().transformToUni(empresa -> configuracionRepository.findByEmpresaIdAndClave(empresaId, clave)
                .onItem().transformToUni(existente -> {
                    if (existente != null) {
                        return Uni.createFrom().failure(new CodigoDuplicadoException(
                            "Ya existe la configuración '" + clave + "' para la empresa " + empresaId));
                    }
                    return guardarYPublicar(empresa, clave, valor);
                }))
            .map(mapper::toResponse);
    }

    /** PATCH — cambia el valor de una clave que ya existe; 404 si todavía no se había creado (ver {@link #crear}). */
    @WithTransaction
    public Uni<ConfiguracionEmpresaResponse> actualizar(UUID empresaId, String clave, String valor) {
        return empresaRepository.findById(empresaId)
            .onItem().ifNull().failWith(() -> new NotFoundException("Empresa no encontrada: " + empresaId))
            .onItem().transformToUni(empresa -> configuracionRepository.findByEmpresaIdAndClave(empresaId, clave)
                .onItem().ifNull().failWith(() -> new NotFoundException(
                    "No existe la configuración '" + clave + "' para la empresa " + empresaId))
                .onItem().transformToUni(existente -> guardarYPublicar(empresa, clave, valor)))
            .map(mapper::toResponse);
    }

    private Uni<ConfiguracionEmpresa> guardarYPublicar(Empresa empresa, String clave, String valor) {
        return configuracionRepository.upsert(empresa.getId(), clave, valor)
            .chain(guardado -> salientes
                .registrar(construirEventoOutbox(empresa, clave, valor,
                    MessagingConstants.EVENT_TYPE_CONFIGURACION_EMPRESA_ESTABLECIDA))
                .replaceWith(guardado));
    }

    @WithSession
    public Uni<List<ConfiguracionEmpresaResponse>> listar(UUID empresaId) {
        return configuracionRepository.findByEmpresaId(empresaId)
            .map(items -> items.stream().map(mapper::toResponse).collect(Collectors.toList()));
    }

    @WithSession
    public Uni<ConfiguracionEmpresaResponse> obtener(UUID empresaId, String clave) {
        return configuracionRepository.findByEmpresaIdAndClave(empresaId, clave)
            .onItem().ifNull().failWith(() ->
                new NotFoundException("No existe la configuración '" + clave + "' para la empresa " + empresaId))
            .map(mapper::toResponse);
    }

    @WithTransaction
    public Uni<Void> eliminar(UUID empresaId, String clave) {
        return empresaRepository.findById(empresaId)
            .onItem().ifNull().failWith(() -> new NotFoundException("Empresa no encontrada: " + empresaId))
            .onItem().transformToUni(empresa -> configuracionRepository.deleteByEmpresaIdAndClave(empresaId, clave)
                .onItem().transformToUni(borrada -> Boolean.TRUE.equals(borrada)
                    ? salientes.registrar(construirEventoOutbox(empresa, clave, null,
                        MessagingConstants.EVENT_TYPE_CONFIGURACION_EMPRESA_ELIMINADA)).replaceWithVoid()
                    : Uni.createFrom().failure(new NotFoundException(
                        "No existe la configuración '" + clave + "' para la empresa " + empresaId))));
    }

    /**
     * Publica un ajuste que NO se guarda en configuracion_empresa porque ya vive en otra columna de la empresa (hoy: el
     * logo, ajuste "ride.logo"): los servicios consumidores lo reciben igual que cualquier otro ajuste.
     */
    public Uni<Void> publicarAjusteDerivado(Empresa empresa, String clave, String valor) {
        return salientes.registrar(construirEventoOutbox(empresa, clave, valor,
            MessagingConstants.EVENT_TYPE_CONFIGURACION_EMPRESA_ESTABLECIDA));
    }

    /** Contraparte de {@link #publicarAjusteDerivado}: los consumidores borran el ajuste. */
    public Uni<Void> retirarAjusteDerivado(Empresa empresa, String clave) {
        return salientes.registrar(construirEventoOutbox(empresa, clave, null,
            MessagingConstants.EVENT_TYPE_CONFIGURACION_EMPRESA_ELIMINADA));
    }

    private EventoSaliente construirEventoOutbox(Empresa empresa, String clave, String valor, String tipoEvento) {
        ConfiguracionEmpresaEventMessage mensaje = new ConfiguracionEmpresaEventMessage(
            UUID.randomUUID(), tipoEvento, empresa.getRuc(), clave, valor, Instant.now());
        return new EventoSaliente(empresa.getId(), tipoEvento, MessagingConstants.CONFIGURACION_EMPRESA_EVENTS_TOPIC,
            serializarAJson(mensaje));
    }

    private String serializarAJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el payload del outbox", e);
        }
    }
}
