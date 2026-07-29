package com.facturacion.firma.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;

import com.facturacion.firma.domain.model.entity.ConfiguracionEmpresa;
import com.facturacion.firma.domain.repository.ConfiguracionEmpresaRepository;
import com.facturacion.firma.infrastructure.persistence.entity.ConfiguracionEmpresaEntity;
import com.facturacion.firma.infrastructure.persistence.mapper.ConfiguracionEmpresaMapper;

@ApplicationScoped
public class ConfiguracionEmpresaRepositoryImpl implements ConfiguracionEmpresaRepository {

    private final ConfiguracionEmpresaPanacheRepository repository;
    private final ConfiguracionEmpresaMapper mapper;

    @Inject
    public ConfiguracionEmpresaRepositoryImpl(ConfiguracionEmpresaPanacheRepository repository,
            ConfiguracionEmpresaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Uni<ConfiguracionEmpresa> upsert(UUID empresaId, String clave, String valor) {
        return repository.find("empresaId = ?1 and clave = ?2", empresaId, clave)
            .<ConfiguracionEmpresaEntity>firstResult()
            .onItem().transformToUni(existente -> {
                if (existente != null) {
                    existente.valor = valor;
                    existente.actualizadoEn = Instant.now();
                    return Uni.createFrom().item(mapper.toDomain(existente));
                }
                Instant ahora = Instant.now();
                ConfiguracionEmpresa nueva = new ConfiguracionEmpresa(UUID.randomUUID(), empresaId, clave, valor,
                    ahora, ahora);
                return repository.persist(mapper.toEntity(nueva)).map(mapper::toDomain);
            });
    }

    @Override
    public Uni<List<ConfiguracionEmpresa>> findByEmpresaId(UUID empresaId) {
        return repository.find("empresaId", empresaId)
            .<ConfiguracionEmpresaEntity>list()
            .map(entities -> entities.stream().map(mapper::toDomain).collect(Collectors.toList()));
    }

    @Override
    public Uni<ConfiguracionEmpresa> findByEmpresaIdAndClave(UUID empresaId, String clave) {
        return repository.find("empresaId = ?1 and clave = ?2", empresaId, clave)
            .<ConfiguracionEmpresaEntity>firstResult()
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }

    @Override
    public Uni<Boolean> deleteByEmpresaIdAndClave(UUID empresaId, String clave) {
        return repository.delete("empresaId = ?1 and clave = ?2", empresaId, clave)
            .map(borrados -> borrados > 0);
    }
}
