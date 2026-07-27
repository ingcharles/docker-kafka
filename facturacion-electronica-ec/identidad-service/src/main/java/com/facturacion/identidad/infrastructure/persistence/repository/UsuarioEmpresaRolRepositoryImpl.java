package com.facturacion.identidad.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;

import com.facturacion.identidad.domain.model.entity.UsuarioEmpresaRol;
import com.facturacion.identidad.domain.repository.UsuarioEmpresaRolRepository;
import com.facturacion.identidad.infrastructure.persistence.entity.UsuarioEmpresaRolEntity;
import com.facturacion.identidad.infrastructure.persistence.mapper.UsuarioEmpresaRolMapper;

@ApplicationScoped
public class UsuarioEmpresaRolRepositoryImpl implements UsuarioEmpresaRolRepository {

    private final UsuarioEmpresaRolPanacheRepository repository;
    private final UsuarioEmpresaRolMapper mapper;

    @Inject
    public UsuarioEmpresaRolRepositoryImpl(UsuarioEmpresaRolPanacheRepository repository,
            UsuarioEmpresaRolMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Uni<UsuarioEmpresaRol> persist(UsuarioEmpresaRol domain) {
        return repository.persist(mapper.toEntity(domain)).map(mapper::toDomain);
    }

    @Override
    public Uni<UsuarioEmpresaRol> update(UsuarioEmpresaRol domain) {
        return repository.<UsuarioEmpresaRolEntity>findById(domain.getId())
            .onItem().ifNotNull().transformToUni(entity -> {
                entity.rol = mapper.map(domain.getRol());
                return Uni.createFrom().item(mapper.toDomain(entity));
            });
    }

    @Override
    public Uni<UsuarioEmpresaRol> findByUsuarioIdAndRuc(UUID usuarioId, String ruc) {
        return repository.find("usuarioId = ?1 and ruc = ?2", usuarioId, ruc)
            .<UsuarioEmpresaRolEntity>firstResult()
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }

    @Override
    public Uni<List<UsuarioEmpresaRol>> findByUsuarioId(UUID usuarioId) {
        return repository.<UsuarioEmpresaRolEntity>find("usuarioId", usuarioId).list()
            .map(entities -> entities.stream().map(mapper::toDomain).collect(Collectors.toList()));
    }
}
