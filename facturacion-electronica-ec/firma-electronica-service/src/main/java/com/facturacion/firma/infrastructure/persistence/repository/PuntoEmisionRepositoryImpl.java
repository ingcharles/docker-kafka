package com.facturacion.firma.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;

import com.facturacion.firma.domain.model.entity.PuntoEmision;
import com.facturacion.firma.domain.repository.PuntoEmisionRepository;
import com.facturacion.firma.infrastructure.persistence.entity.PuntoEmisionEntity;
import com.facturacion.firma.infrastructure.persistence.mapper.PuntoEmisionMapper;

@ApplicationScoped
public class PuntoEmisionRepositoryImpl implements PuntoEmisionRepository {

    private final PuntoEmisionPanacheRepository repository;
    private final PuntoEmisionMapper mapper;

    @Inject
    public PuntoEmisionRepositoryImpl(PuntoEmisionPanacheRepository repository, PuntoEmisionMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Uni<PuntoEmision> persist(PuntoEmision domain) {
        return repository.persist(mapper.toEntity(domain)).map(mapper::toDomain);
    }

    @Override
    public Uni<PuntoEmision> update(PuntoEmision domain) {
        return repository.<PuntoEmisionEntity>findById(domain.getId())
            .onItem().ifNotNull().transformToUni(entity -> {
                entity.descripcion = domain.getDescripcion();
                entity.activo = domain.isActivo();
                entity.actualizadoEn = Instant.now();
                return Uni.createFrom().item(mapper.toDomain(entity));
            });
    }

    @Override
    public Uni<PuntoEmision> findById(UUID id) {
        return repository.<PuntoEmisionEntity>findById(id)
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }

    @Override
    public Uni<List<PuntoEmision>> findByEstablecimientoId(UUID establecimientoId) {
        return repository.<PuntoEmisionEntity>find("establecimientoId", establecimientoId).list()
            .map(entities -> entities.stream().map(mapper::toDomain).collect(Collectors.toList()));
    }

    @Override
    public Uni<Boolean> existsByEstablecimientoIdAndCodigo(UUID establecimientoId, String codigo) {
        return repository.count("establecimientoId = ?1 and codigo = ?2", establecimientoId, codigo)
            .map(count -> count > 0);
    }
}
