package com.facturacion.inventario.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Uni;

import com.facturacion.inventario.domain.model.entity.Categoria;
import com.facturacion.inventario.domain.model.vo.PageResult;
import com.facturacion.inventario.domain.repository.CategoriaRepository;
import com.facturacion.inventario.infrastructure.persistence.entity.CategoriaEntity;
import com.facturacion.inventario.infrastructure.persistence.mapper.CategoriaMapper;

@ApplicationScoped
public class CategoriaRepositoryImpl implements CategoriaRepository {

    private final CategoriaPanacheRepository repository;
    private final CategoriaMapper mapper;

    @Inject
    public CategoriaRepositoryImpl(CategoriaPanacheRepository repository, CategoriaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Uni<Categoria> persist(Categoria domain) {
        return repository.persist(mapper.toEntity(domain)).map(mapper::toDomain);
    }

    @Override
    public Uni<Categoria> findById(UUID id) {
        return repository.<CategoriaEntity>findById(id)
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }

    @Override
    public Uni<PageResult<Categoria>> findPaged(int page, int limit) {
        var panacheQuery = repository.findAll().page(Page.of(page, limit));
        Uni<List<CategoriaEntity>> itemsUni = panacheQuery.list();
        Uni<Long> countUni = panacheQuery.count();

        return Uni.combine().all().unis(itemsUni, countUni)
            .asTuple()
            .map(tuple -> new PageResult<>(
                tuple.getItem1().stream().map(mapper::toDomain).collect(Collectors.toList()),
                tuple.getItem2()));
    }
}
