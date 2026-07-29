package com.facturacion.firma.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;

import com.facturacion.firma.domain.model.entity.Establecimiento;
import com.facturacion.firma.domain.repository.EstablecimientoRepository;
import com.facturacion.firma.infrastructure.persistence.entity.EstablecimientoEntity;
import com.facturacion.firma.infrastructure.persistence.mapper.EstablecimientoMapper;

@ApplicationScoped
public class EstablecimientoRepositoryImpl implements EstablecimientoRepository {

    private final EstablecimientoPanacheRepository repository;
    private final EstablecimientoMapper mapper;

    @Inject
    public EstablecimientoRepositoryImpl(EstablecimientoPanacheRepository repository,
            EstablecimientoMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Uni<Establecimiento> persist(Establecimiento domain) {
        return repository.persist(mapper.toEntity(domain)).map(mapper::toDomain);
    }

    @Override
    public Uni<Establecimiento> update(Establecimiento domain) {
        return repository.<EstablecimientoEntity>findById(domain.getId())
            .onItem().ifNotNull().transformToUni(entity -> {
                entity.direccion = domain.getDireccion();
                entity.tipo = domain.getTipo();
                entity.activo = domain.isActivo();
                entity.actualizadoEn = Instant.now();
                return Uni.createFrom().item(mapper.toDomain(entity));
            });
    }

    @Override
    public Uni<Establecimiento> findById(UUID id) {
        return repository.<EstablecimientoEntity>findById(id)
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }

    @Override
    public Uni<List<Establecimiento>> findByEmpresaId(UUID empresaId) {
        return repository.<EstablecimientoEntity>find("empresaId", empresaId).list()
            .map(entities -> entities.stream().map(mapper::toDomain).collect(Collectors.toList()));
    }

    @Override
    public Uni<Boolean> existsByEmpresaIdAndCodigo(UUID empresaId, String codigo) {
        return repository.count("empresaId = ?1 and codigo = ?2", empresaId, codigo).map(count -> count > 0);
    }
}
