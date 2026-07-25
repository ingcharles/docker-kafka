package com.facturacion.firma.application.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.facturacion.firma.application.dto.ConfiguracionEmisorResponse;
import com.facturacion.firma.domain.repository.ConfiguracionEmisorRepository;
import com.facturacion.firma.domain.repository.EmisorRepository;
import com.facturacion.firma.infrastructure.persistence.mapper.ConfiguracionEmisorMapper;
import com.facturacion.firma.shared.exception.NotFoundException;

/**
 * Ajustes opcionales por emisor, guardados como pares clave/valor — ver la
 * migración V3 para el razonamiento de por qué esto es una tabla genérica
 * separada en vez de columnas nuevas en {@code Emisor} cada vez que hace
 * falta un ajuste más.
 */
@ApplicationScoped
public class ConfiguracionEmisorService {

    private final EmisorRepository emisorRepository;
    private final ConfiguracionEmisorRepository configuracionRepository;
    private final ConfiguracionEmisorMapper mapper;

    @Inject
    public ConfiguracionEmisorService(EmisorRepository emisorRepository,
            ConfiguracionEmisorRepository configuracionRepository, ConfiguracionEmisorMapper mapper) {
        this.emisorRepository = emisorRepository;
        this.configuracionRepository = configuracionRepository;
        this.mapper = mapper;
    }

    @WithTransaction
    public Uni<ConfiguracionEmisorResponse> establecer(UUID emisorId, String clave, String valor) {
        return emisorRepository.findById(emisorId)
            .onItem().ifNull().failWith(() -> new NotFoundException("Emisor no encontrado: " + emisorId))
            .onItem().transformToUni(ignorado -> configuracionRepository.upsert(emisorId, clave, valor))
            .map(mapper::toResponse);
    }

    @WithSession
    public Uni<List<ConfiguracionEmisorResponse>> listar(UUID emisorId) {
        return configuracionRepository.findByEmisorId(emisorId)
            .map(items -> items.stream().map(mapper::toResponse).collect(Collectors.toList()));
    }

    @WithSession
    public Uni<ConfiguracionEmisorResponse> obtener(UUID emisorId, String clave) {
        return configuracionRepository.findByEmisorIdAndClave(emisorId, clave)
            .onItem().ifNull().failWith(() ->
                new NotFoundException("No existe la configuración '" + clave + "' para el emisor " + emisorId))
            .map(mapper::toResponse);
    }

    @WithTransaction
    public Uni<Void> eliminar(UUID emisorId, String clave) {
        return configuracionRepository.deleteByEmisorIdAndClave(emisorId, clave)
            .onItem().transformToUni(borrada -> Boolean.TRUE.equals(borrada)
                ? Uni.createFrom().voidItem()
                : Uni.createFrom().failure(new NotFoundException(
                    "No existe la configuración '" + clave + "' para el emisor " + emisorId)));
    }
}
