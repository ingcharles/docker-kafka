package com.facturacion.identidad.infrastructure.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;

import com.facturacion.identidad.domain.model.entity.EmisorLocal;
import com.facturacion.identidad.domain.repository.EmisorLocalRepository;
import com.facturacion.identidad.infrastructure.persistence.entity.EmisorLocalEntity;
import com.facturacion.identidad.infrastructure.persistence.mapper.EmisorLocalMapper;

@ApplicationScoped
public class EmisorLocalRepositoryImpl implements EmisorLocalRepository {

    private final EmisorLocalPanacheRepository repository;
    private final EmisorLocalMapper mapper;

    @Inject
    public EmisorLocalRepositoryImpl(EmisorLocalPanacheRepository repository, EmisorLocalMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Uni<EmisorLocal> upsert(EmisorLocal domain) {
        return repository.<EmisorLocalEntity>findById(domain.getId())
            .onItem().transformToUni(existente -> {
                if (existente == null) {
                    return repository.persist(mapper.toEntity(domain)).map(mapper::toDomain);
                }
                existente.ruc = domain.getRuc();
                existente.razonSocial = domain.getRazonSocial();
                existente.activo = domain.isActivo();
                existente.actualizadoEn = domain.getActualizadoEn();
                return Uni.createFrom().item(mapper.toDomain(existente));
            });
    }

    @Override
    public Uni<EmisorLocal> findByRuc(String ruc) {
        return repository.find("ruc", ruc).firstResult()
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }
}
