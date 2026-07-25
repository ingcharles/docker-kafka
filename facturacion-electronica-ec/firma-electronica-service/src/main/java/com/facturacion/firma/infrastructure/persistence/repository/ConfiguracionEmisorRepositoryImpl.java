package com.facturacion.firma.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;

import com.facturacion.firma.domain.model.entity.ConfiguracionEmisor;
import com.facturacion.firma.domain.repository.ConfiguracionEmisorRepository;
import com.facturacion.firma.infrastructure.persistence.entity.ConfiguracionEmisorEntity;
import com.facturacion.firma.infrastructure.persistence.mapper.ConfiguracionEmisorMapper;

@ApplicationScoped
public class ConfiguracionEmisorRepositoryImpl implements ConfiguracionEmisorRepository {

    private final ConfiguracionEmisorPanacheRepository repository;
    private final ConfiguracionEmisorMapper mapper;

    @Inject
    public ConfiguracionEmisorRepositoryImpl(ConfiguracionEmisorPanacheRepository repository,
            ConfiguracionEmisorMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Uni<ConfiguracionEmisor> upsert(UUID emisorId, String clave, String valor) {
        return repository.find("emisorId = ?1 and clave = ?2", emisorId, clave)
            .<ConfiguracionEmisorEntity>firstResult()
            .onItem().transformToUni(existente -> {
                if (existente != null) {
                    existente.valor = valor;
                    existente.actualizadoEn = Instant.now();
                    return Uni.createFrom().item(mapper.toDomain(existente));
                }
                Instant ahora = Instant.now();
                ConfiguracionEmisor nueva = new ConfiguracionEmisor(UUID.randomUUID(), emisorId, clave, valor,
                    ahora, ahora);
                return repository.persist(mapper.toEntity(nueva)).map(mapper::toDomain);
            });
    }

    @Override
    public Uni<List<ConfiguracionEmisor>> findByEmisorId(UUID emisorId) {
        return repository.find("emisorId", emisorId)
            .<ConfiguracionEmisorEntity>list()
            .map(entities -> entities.stream().map(mapper::toDomain).collect(Collectors.toList()));
    }

    @Override
    public Uni<ConfiguracionEmisor> findByEmisorIdAndClave(UUID emisorId, String clave) {
        return repository.find("emisorId = ?1 and clave = ?2", emisorId, clave)
            .<ConfiguracionEmisorEntity>firstResult()
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }

    @Override
    public Uni<Boolean> deleteByEmisorIdAndClave(UUID emisorId, String clave) {
        return repository.delete("emisorId = ?1 and clave = ?2", emisorId, clave)
            .map(borrados -> borrados > 0);
    }
}
