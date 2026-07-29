package com.facturacion.comprobantes.infrastructure.persistence.repository;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;

import com.facturacion.comprobantes.domain.model.entity.PuntoEmisionLocal;
import com.facturacion.comprobantes.domain.repository.PuntoEmisionLocalRepository;
import com.facturacion.comprobantes.infrastructure.persistence.entity.PuntoEmisionLocalEntity;
import com.facturacion.comprobantes.infrastructure.persistence.mapper.PuntoEmisionLocalMapper;

@ApplicationScoped
public class PuntoEmisionLocalRepositoryImpl implements PuntoEmisionLocalRepository {

    private final PuntoEmisionLocalPanacheRepository repository;
    private final PuntoEmisionLocalMapper mapper;

    @Inject
    public PuntoEmisionLocalRepositoryImpl(PuntoEmisionLocalPanacheRepository repository,
            PuntoEmisionLocalMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Uni<PuntoEmisionLocal> upsert(PuntoEmisionLocal domain) {
        return repository.<PuntoEmisionLocalEntity>find("ruc = ?1 and establecimientoCodigo = ?2 and codigo = ?3",
                domain.getRuc(), domain.getEstablecimientoCodigo(), domain.getCodigo())
            .firstResult()
            .onItem().transformToUni(existente -> {
                if (existente == null) {
                    PuntoEmisionLocalEntity nueva = mapper.toEntity(domain);
                    nueva.id = UUID.randomUUID();
                    return repository.persist(nueva).map(mapper::toDomain);
                }
                existente.activo = domain.isActivo();
                existente.actualizadoEn = domain.getActualizadoEn();
                return Uni.createFrom().item(mapper.toDomain(existente));
            });
    }

    @Override
    public Uni<PuntoEmisionLocal> findByRucAndEstablecimientoCodigoAndCodigo(String ruc,
            String establecimientoCodigo, String codigo) {
        return repository.<PuntoEmisionLocalEntity>find("ruc = ?1 and establecimientoCodigo = ?2 and codigo = ?3",
                ruc, establecimientoCodigo, codigo)
            .firstResult()
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }
}
