package com.facturacion.identidad.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;

import com.facturacion.identidad.domain.model.entity.ApiKey;
import com.facturacion.identidad.domain.repository.ApiKeyRepository;
import com.facturacion.identidad.infrastructure.persistence.entity.ApiKeyEntity;
import com.facturacion.identidad.infrastructure.persistence.mapper.ApiKeyMapper;

@ApplicationScoped
public class ApiKeyRepositoryImpl implements ApiKeyRepository {

    private final ApiKeyPanacheRepository repository;
    private final ApiKeyMapper mapper;

    @Inject
    public ApiKeyRepositoryImpl(ApiKeyPanacheRepository repository, ApiKeyMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Uni<ApiKey> persist(ApiKey domain) {
        return repository.persist(mapper.toEntity(domain)).map(mapper::toDomain);
    }

    @Override
    public Uni<ApiKey> findById(UUID id) {
        return repository.<ApiKeyEntity>findById(id)
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }

    @Override
    public Uni<List<ApiKey>> findByUsuarioId(UUID usuarioId) {
        return repository.<ApiKeyEntity>find("usuarioId", usuarioId).list()
            .map(entities -> entities.stream().map(mapper::toDomain).collect(Collectors.toList()));
    }

    @Override
    public Uni<Boolean> existsByUsuarioIdAndNombre(UUID usuarioId, String nombre) {
        return repository.count("usuarioId = ?1 and nombre = ?2", usuarioId, nombre).map(count -> count > 0);
    }

    @Override
    public Uni<Integer> revocar(UUID id, UUID usuarioId) {
        return repository.update("revocada = true, revocadaEn = ?1 where id = ?2 and usuarioId = ?3 and revocada = false",
            Instant.now(), id, usuarioId);
    }

    @Override
    public Uni<List<ApiKey>> findVigentesByUsuarioIdAndRuc(UUID usuarioId, String ruc) {
        return repository.<ApiKeyEntity>find(
                "usuarioId = ?1 and ruc = ?2 and revocada = false and fechaExpiracion > ?3", usuarioId, ruc, Instant.now())
            .list()
            .map(entities -> entities.stream().map(mapper::toDomain).collect(Collectors.toList()));
    }
}
