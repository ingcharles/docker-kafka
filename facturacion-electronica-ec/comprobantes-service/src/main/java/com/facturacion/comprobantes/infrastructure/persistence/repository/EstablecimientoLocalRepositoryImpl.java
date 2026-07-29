package com.facturacion.comprobantes.infrastructure.persistence.repository;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;

import com.facturacion.comprobantes.domain.model.entity.EstablecimientoLocal;
import com.facturacion.comprobantes.domain.repository.EstablecimientoLocalRepository;
import com.facturacion.comprobantes.infrastructure.persistence.entity.EstablecimientoLocalEntity;
import com.facturacion.comprobantes.infrastructure.persistence.mapper.EstablecimientoLocalMapper;

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
    public Uni<EstablecimientoLocal> upsert(EstablecimientoLocal domain) {
        return repository.<EstablecimientoLocalEntity>find("ruc = ?1 and codigo = ?2", domain.getRuc(),
                domain.getCodigo())
            .firstResult()
            .onItem().transformToUni(existente -> {
                if (existente == null) {
                    EstablecimientoLocalEntity nueva = mapper.toEntity(domain);
                    nueva.id = UUID.randomUUID();
                    return repository.persist(nueva).map(mapper::toDomain);
                }
                existente.activo = domain.isActivo();
                existente.actualizadoEn = domain.getActualizadoEn();
                return Uni.createFrom().item(mapper.toDomain(existente));
            });
    }

    @Override
    public Uni<EstablecimientoLocal> findByRucAndCodigo(String ruc, String codigo) {
        return repository.<EstablecimientoLocalEntity>find("ruc = ?1 and codigo = ?2", ruc, codigo).firstResult()
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }
}
