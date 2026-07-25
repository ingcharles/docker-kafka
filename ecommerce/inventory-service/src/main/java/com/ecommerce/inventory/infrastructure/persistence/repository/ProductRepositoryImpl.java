package com.ecommerce.inventory.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Uni;

import com.ecommerce.inventory.domain.model.entity.Product;
import com.ecommerce.inventory.domain.model.vo.PageResult;
import com.ecommerce.inventory.domain.repository.ProductRepository;
import com.ecommerce.inventory.infrastructure.persistence.entity.ProductEntity;
import com.ecommerce.inventory.infrastructure.persistence.mapper.ProductMapper;

@ApplicationScoped
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductPanacheRepository repository;
    private final ProductMapper mapper;

    @Inject
    public ProductRepositoryImpl(ProductPanacheRepository repository, ProductMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Uni<Product> persist(Product domain) {
        return repository.persist(mapper.toEntity(domain)).map(mapper::toDomain);
    }

    @Override
    public Uni<Product> findById(UUID id) {
        return repository.findById(id).map(entity -> entity == null ? null : mapper.toDomain(entity));
    }

    @Override
    public Uni<PageResult<Product>> findPaged(int page, int limit) {
        var query = repository.findAll().page(Page.of(page, limit));
        Uni<List<ProductEntity>> itemsUni = query.list();
        Uni<Long> countUni = query.count();
        return Uni.combine().all().unis(itemsUni, countUni).asTuple()
            .map(tuple -> new PageResult<>(
                tuple.getItem1().stream().map(mapper::toDomain).collect(Collectors.toList()),
                tuple.getItem2()));
    }

    @Override
    public Uni<Boolean> tryReserve(UUID productId, int quantity) {
        return repository.update(
                "availableQuantity = availableQuantity - ?1, reservedQuantity = reservedQuantity + ?1, "
                    + "updatedAt = ?2 where id = ?3 and availableQuantity >= ?1",
                quantity, Instant.now(), productId)
            .map(rowsUpdated -> rowsUpdated > 0);
    }

    @Override
    public Uni<Void> release(UUID productId, int quantity) {
        return repository.update(
                "availableQuantity = availableQuantity + ?1, reservedQuantity = reservedQuantity - ?1, "
                    + "updatedAt = ?2 where id = ?3",
                quantity, Instant.now(), productId)
            .replaceWithVoid();
    }
}
