package com.facturacion.identidad.infrastructure.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;

import com.facturacion.identidad.domain.model.entity.EmpresaLocal;
import com.facturacion.identidad.domain.repository.EmpresaLocalRepository;
import com.facturacion.identidad.infrastructure.persistence.entity.EmpresaLocalEntity;
import com.facturacion.identidad.infrastructure.persistence.mapper.EmpresaLocalMapper;

@ApplicationScoped
public class EmpresaLocalRepositoryImpl implements EmpresaLocalRepository {

    private final EmpresaLocalPanacheRepository repository;
    private final EmpresaLocalMapper mapper;

    @Inject
    public EmpresaLocalRepositoryImpl(EmpresaLocalPanacheRepository repository, EmpresaLocalMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Uni<EmpresaLocal> insertarOActualizar(EmpresaLocal domain) {
        return repository.<EmpresaLocalEntity>findById(domain.getId())
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
    public Uni<EmpresaLocal> findByRuc(String ruc) {
        return repository.find("ruc", ruc).firstResult()
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }
}
