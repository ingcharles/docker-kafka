package com.facturacion.identidad.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;

import com.facturacion.identidad.domain.model.entity.Usuario;
import com.facturacion.identidad.domain.repository.UsuarioRepository;
import com.facturacion.identidad.infrastructure.persistence.entity.UsuarioEntity;
import com.facturacion.identidad.infrastructure.persistence.mapper.UsuarioMapper;

@ApplicationScoped
public class UsuarioRepositoryImpl implements UsuarioRepository {

    private final UsuarioPanacheRepository repository;
    private final UsuarioMapper mapper;

    @Inject
    public UsuarioRepositoryImpl(UsuarioPanacheRepository repository, UsuarioMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Uni<Usuario> persist(Usuario domain) {
        return repository.persist(mapper.toEntity(domain)).map(mapper::toDomain);
    }

    @Override
    public Uni<Usuario> update(Usuario domain) {
        return repository.<UsuarioEntity>findById(domain.getId())
            .onItem().ifNotNull().transformToUni(entity -> {
                entity.nombres = domain.getNombres();
                entity.apellidos = domain.getApellidos();
                entity.passwordHash = domain.getPasswordHash();
                entity.activo = domain.isActivo();
                entity.actualizadoEn = domain.getActualizadoEn();
                return Uni.createFrom().item(mapper.toDomain(entity));
            });
    }

    @Override
    public Uni<Usuario> findById(UUID id) {
        return repository.<UsuarioEntity>findById(id)
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }

    @Override
    public Uni<Usuario> findByEmail(String email) {
        return repository.find("email", email).<UsuarioEntity>firstResult()
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }

    @Override
    public Uni<Boolean> existsByEmail(String email) {
        return repository.count("email", email).map(count -> count > 0);
    }

    @Override
    public Uni<List<Usuario>> findByIds(List<UUID> ids) {
        if (ids.isEmpty()) {
            return Uni.createFrom().item(List.of());
        }
        return repository.<UsuarioEntity>find("id in ?1", ids).list()
            .map(entities -> entities.stream().map(mapper::toDomain).collect(Collectors.toList()));
    }
}
