package com.facturacion.identidad.infrastructure.persistence.repository;

import java.util.UUID;

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
}
