package com.facturacion.inventario.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;

import com.facturacion.inventario.domain.model.entity.MovimientoKardex;
import com.facturacion.commons.paging.PageResult;
import com.facturacion.inventario.domain.repository.MovimientoKardexRepository;
import com.facturacion.inventario.infrastructure.persistence.entity.MovimientoKardexEntity;
import com.facturacion.inventario.infrastructure.persistence.mapper.MovimientoKardexMapper;

@ApplicationScoped
public class MovimientoKardexRepositoryImpl implements MovimientoKardexRepository {

    private final MovimientoKardexPanacheRepository repository;
    private final MovimientoKardexMapper mapper;

    @Inject
    public MovimientoKardexRepositoryImpl(MovimientoKardexPanacheRepository repository,
            MovimientoKardexMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Uni<MovimientoKardex> persist(MovimientoKardex domain) {
        return repository.persist(mapper.toEntity(domain)).map(mapper::toDomain);
    }

    @Override
    public Uni<MovimientoKardex> findUltimoByItemId(UUID itemId) {
        return repository.find("itemId = ?1", Sort.descending("creadoEn"), itemId)
            .<MovimientoKardexEntity>firstResult()
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }

    @Override
    public Uni<PageResult<MovimientoKardex>> findPagedByItemId(UUID itemId, int page, int limit) {
        var panacheQuery = repository.find("itemId = ?1", Sort.descending("creadoEn"), itemId)
            .page(Page.of(page, limit));
        Uni<List<MovimientoKardexEntity>> itemsUni = panacheQuery.list();
        Uni<Long> countUni = panacheQuery.count();

        return Uni.combine().all().unis(itemsUni, countUni)
            .asTuple()
            .map(tuple -> new PageResult<>(
                tuple.getItem1().stream().map(mapper::toDomain).collect(Collectors.toList()),
                tuple.getItem2()));
    }
}
