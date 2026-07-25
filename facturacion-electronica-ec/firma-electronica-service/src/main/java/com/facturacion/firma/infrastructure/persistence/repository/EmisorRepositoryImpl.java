package com.facturacion.firma.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Uni;

import com.facturacion.firma.domain.model.entity.Emisor;
import com.facturacion.firma.domain.model.vo.PageResult;
import com.facturacion.firma.domain.repository.EmisorRepository;
import com.facturacion.firma.infrastructure.persistence.entity.EmisorEntity;
import com.facturacion.firma.infrastructure.persistence.mapper.EmisorMapper;

@ApplicationScoped
public class EmisorRepositoryImpl implements EmisorRepository {

    private final EmisorPanacheRepository repository;
    private final EmisorMapper mapper;

    @Inject
    public EmisorRepositoryImpl(EmisorPanacheRepository repository, EmisorMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Uni<Emisor> persist(Emisor domain) {
        return repository.persist(mapper.toEntity(domain)).map(mapper::toDomain);
    }

    @Override
    public Uni<Emisor> update(Emisor domain) {
        return repository.<EmisorEntity>findById(domain.getId())
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
    public Uni<Emisor> findById(UUID id) {
        return repository.<EmisorEntity>findById(id)
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }

    @Override
    public Uni<Emisor> findByRuc(String ruc) {
        return repository.<EmisorEntity>find("ruc", ruc).firstResult()
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }

    @Override
    public Uni<Boolean> existsByRuc(String ruc) {
        return repository.count("ruc", ruc).map(count -> count > 0);
    }

    @Override
    public Uni<PageResult<Emisor>> findPaged(int page, int limit) {
        var panacheQuery = repository.findAll().page(Page.of(page, limit));
        Uni<List<EmisorEntity>> itemsUni = panacheQuery.list();
        Uni<Long> countUni = panacheQuery.count();

        return Uni.combine().all().unis(itemsUni, countUni)
            .asTuple()
            .map(tuple -> new PageResult<>(
                tuple.getItem1().stream().map(mapper::toDomain).collect(Collectors.toList()),
                tuple.getItem2()));
    }
}
