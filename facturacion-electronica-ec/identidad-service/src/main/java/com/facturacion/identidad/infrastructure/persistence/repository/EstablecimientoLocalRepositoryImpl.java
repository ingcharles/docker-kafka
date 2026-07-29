package com.facturacion.identidad.infrastructure.persistence.repository;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;

import com.facturacion.identidad.domain.model.entity.EstablecimientoLocal;
import com.facturacion.identidad.domain.repository.EstablecimientoLocalRepository;
import com.facturacion.identidad.infrastructure.persistence.entity.EstablecimientoLocalEntity;
import com.facturacion.identidad.infrastructure.persistence.mapper.EstablecimientoLocalMapper;

@ApplicationScoped
public class EstablecimientoLocalRepositoryImpl implements EstablecimientoLocalRepository {

    private final EstablecimientoLocalPanacheRepository repository;
    private final EstablecimientoLocalMapper mapper;

    @Inject
    public EstablecimientoLocalRepositoryImpl(EstablecimientoLocalPanacheRepository repository,
            EstablecimientoLocalMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Uni<Void> upsert(EstablecimientoLocal domain) {
        return repository.<EstablecimientoLocalEntity>find("ruc = ?1 and codigo = ?2", domain.getRuc(),
                domain.getCodigo())
            .firstResult()
            .onItem().transformToUni(existente -> {
                if (existente == null) {
                    EstablecimientoLocalEntity nueva = mapper.toEntity(domain);
                    nueva.id = UUID.randomUUID();
                    return repository.persist(nueva).replaceWithVoid();
                }
                existente.activo = domain.isActivo();
                existente.actualizadoEn = domain.getActualizadoEn();
                return Uni.createFrom().voidItem();
            });
    }

    @Override
    public Uni<EstablecimientoLocal> findByRucAndCodigo(String ruc, String codigo) {
        return repository.<EstablecimientoLocalEntity>find("ruc = ?1 and codigo = ?2", ruc, codigo).firstResult()
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }
}
