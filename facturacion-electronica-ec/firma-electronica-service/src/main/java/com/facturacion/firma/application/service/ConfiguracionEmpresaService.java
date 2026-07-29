package com.facturacion.firma.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.facturacion.firma.application.dto.ConfiguracionEmpresaResponse;
import com.facturacion.firma.application.event.ConfiguracionEmpresaEventMessage;
import com.facturacion.firma.domain.model.entity.Empresa;
import com.facturacion.firma.domain.repository.ConfiguracionEmpresaRepository;
import com.facturacion.firma.domain.repository.EmpresaRepository;
import com.facturacion.firma.infrastructure.outbox.OutboxEventEntity;
import com.facturacion.firma.infrastructure.outbox.OutboxEventRepository;
import com.facturacion.firma.infrastructure.persistence.mapper.ConfiguracionEmpresaMapper;
import com.facturacion.firma.shared.constant.MessagingConstants;
import com.facturacion.firma.shared.exception.NotFoundException;

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
    private final ConfiguracionEmpresaMapper mapper;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Inject
    public ConfiguracionEmpresaService(EmpresaRepository empresaRepository,
            ConfiguracionEmpresaRepository configuracionRepository, ConfiguracionEmpresaMapper mapper,
            OutboxEventRepository outboxRepository, ObjectMapper objectMapper) {
        this.empresaRepository = empresaRepository;
        this.configuracionRepository = configuracionRepository;
        this.mapper = mapper;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @WithTransaction
    public Uni<ConfiguracionEmpresaResponse> establecer(UUID empresaId, String clave, String valor) {
        return empresaRepository.findById(empresaId)
            .onItem().ifNull().failWith(() -> new NotFoundException("Empresa no encontrada: " + empresaId))
            .onItem().transformToUni(empresa -> configuracionRepository.upsert(empresaId, clave, valor)
                .chain(guardado -> outboxRepository
                    .persist(construirEventoOutbox(empresa, clave, valor,
                        MessagingConstants.EVENT_TYPE_CONFIGURACION_EMPRESA_ESTABLECIDA))
                    .replaceWith(guardado)))
            .map(mapper::toResponse);
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
                    ? outboxRepository.persist(construirEventoOutbox(empresa, clave, null,
                        MessagingConstants.EVENT_TYPE_CONFIGURACION_EMPRESA_ELIMINADA)).replaceWithVoid()
                    : Uni.createFrom().failure(new NotFoundException(
                        "No existe la configuración '" + clave + "' para la empresa " + empresaId))));
    }

    private OutboxEventEntity construirEventoOutbox(Empresa empresa, String clave, String valor, String tipoEvento) {
        ConfiguracionEmpresaEventMessage mensaje = new ConfiguracionEmpresaEventMessage(
            UUID.randomUUID(), tipoEvento, empresa.getRuc(), clave, valor, Instant.now());
        return OutboxEventEntity.of(empresa.getId(), tipoEvento, MessagingConstants.CONFIGURACION_EMPRESA_EVENTS_TOPIC,
            writeValueAsString(mensaje));
    }

    private String writeValueAsString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el payload del outbox", e);
        }
    }
}
