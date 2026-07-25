package com.facturacion.comprobantes.infrastructure.persistence.repository;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;

import com.facturacion.comprobantes.domain.model.entity.EmisorLocal;
import com.facturacion.comprobantes.domain.repository.EmisorLocalRepository;
import com.facturacion.comprobantes.infrastructure.persistence.entity.EmisorLocalEntity;
import com.facturacion.comprobantes.infrastructure.persistence.mapper.EmisorLocalMapper;

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
                existente.nombreComercial = domain.getNombreComercial();
                existente.direccionMatriz = domain.getDireccionMatriz();
                existente.obligadoContabilidad = domain.isObligadoContabilidad();
                existente.contribuyenteEspecial = domain.isContribuyenteEspecial();
                existente.numeroContribuyenteEspecial = domain.getNumeroContribuyenteEspecial();
                existente.esGranContribuyente = domain.isEsGranContribuyente();
                existente.numeroResolucionGranContribuyente = domain.getNumeroResolucionGranContribuyente();
                existente.vendeCombustibles = domain.isVendeCombustibles();
                existente.ambiente = mapper.map(domain.getAmbiente());
                existente.establecimientoDefecto = domain.getEstablecimientoDefecto();
                existente.puntoEmisionDefecto = domain.getPuntoEmisionDefecto();
                existente.activo = domain.isActivo();
                existente.actualizadoEn = domain.getActualizadoEn();
                return Uni.createFrom().item(mapper.toDomain(existente));
            });
    }

    @Override
    public Uni<EmisorLocal> findById(UUID id) {
        return repository.<EmisorLocalEntity>findById(id)
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }
}
