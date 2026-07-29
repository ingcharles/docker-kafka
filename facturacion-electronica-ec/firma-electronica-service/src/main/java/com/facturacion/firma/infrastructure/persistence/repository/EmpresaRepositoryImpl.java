package com.facturacion.firma.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Uni;

import com.facturacion.firma.domain.model.entity.Empresa;
import com.facturacion.firma.domain.model.vo.PageResult;
import com.facturacion.firma.domain.repository.EmpresaRepository;
import com.facturacion.firma.infrastructure.persistence.entity.EmpresaEntity;
import com.facturacion.firma.infrastructure.persistence.mapper.EmpresaMapper;

@ApplicationScoped
public class EmpresaRepositoryImpl implements EmpresaRepository {

    private final EmpresaPanacheRepository repository;
    private final EmpresaMapper mapper;

    @Inject
    public EmpresaRepositoryImpl(EmpresaPanacheRepository repository, EmpresaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Uni<Empresa> persist(Empresa domain) {
        return repository.persist(mapper.toEntity(domain)).map(mapper::toDomain);
    }

    @Override
    public Uni<Empresa> update(Empresa domain) {
        return repository.<EmpresaEntity>findById(domain.getId())
            .onItem().ifNotNull().transformToUni(entity -> {
                entity.razonSocial = domain.getRazonSocial();
                entity.nombreComercial = domain.getNombreComercial();
                entity.direccionMatriz = domain.getDireccionMatriz();
                entity.obligadoContabilidad = domain.isObligadoContabilidad();
                entity.contribuyenteEspecial = domain.isContribuyenteEspecial();
                entity.numeroContribuyenteEspecial = domain.getNumeroContribuyenteEspecial();
                entity.esGranContribuyente = domain.isEsGranContribuyente();
                entity.numeroResolucionGranContribuyente = domain.getNumeroResolucionGranContribuyente();
                entity.vendeCombustibles = domain.isVendeCombustibles();
                entity.ambiente = mapper.map(domain.getAmbiente());
                entity.establecimientoDefecto = domain.getEstablecimientoDefecto();
                entity.puntoEmisionDefecto = domain.getPuntoEmisionDefecto();
                entity.activo = domain.isActivo();
                entity.actualizadoEn = Instant.now();
                return Uni.createFrom().item(mapper.toDomain(entity));
            });
    }

    @Override
    public Uni<Empresa> findById(UUID id) {
        return repository.<EmpresaEntity>findById(id)
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }

    @Override
    public Uni<Empresa> findByRuc(String ruc) {
        return repository.<EmpresaEntity>find("ruc", ruc).firstResult()
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }

    @Override
    public Uni<Boolean> existsByRuc(String ruc) {
        return repository.count("ruc", ruc).map(count -> count > 0);
    }

    @Override
    public Uni<PageResult<Empresa>> findPaged(int page, int limit) {
        var panacheQuery = repository.findAll().page(Page.of(page, limit));
        Uni<List<EmpresaEntity>> itemsUni = panacheQuery.list();
        Uni<Long> countUni = panacheQuery.count();

        return Uni.combine().all().unis(itemsUni, countUni)
            .asTuple()
            .map(tuple -> new PageResult<>(
                tuple.getItem1().stream().map(mapper::toDomain).collect(Collectors.toList()),
                tuple.getItem2()));
    }
}
