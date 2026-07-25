package com.ecommerce.inventory.application.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.ecommerce.inventory.application.dto.CreateProductRequest;
import com.ecommerce.inventory.application.dto.ProductResponse;
import com.ecommerce.inventory.domain.model.entity.Product;
import com.ecommerce.inventory.domain.model.vo.PageResult;
import com.ecommerce.inventory.domain.repository.ProductRepository;
import com.ecommerce.inventory.infrastructure.persistence.mapper.ProductMapper;
import com.ecommerce.inventory.shared.exception.NotFoundException;
import com.ecommerce.inventory.shared.responses.PagedResponse;

@ApplicationScoped
public class ProductService {

    private final ProductRepository repository;
    private final ProductMapper mapper;

    @Inject
    public ProductService(ProductRepository repository, ProductMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @WithTransaction
    public Uni<ProductResponse> createProduct(@Valid CreateProductRequest request) {
        Product product = Product.createNew(request.getSku(), request.getName(), request.getUnitPrice(),
            request.getInitialQuantity());
        return repository.persist(product).map(mapper::toResponse);
    }

    @WithSession
    public Uni<ProductResponse> getById(UUID id) {
        return repository.findById(id)
            .onItem().ifNull().failWith(() -> new NotFoundException("Product not found: " + id))
            .map(mapper::toResponse);
    }

    @WithSession
    public Uni<PagedResponse<ProductResponse>> listPaged(int page, int limit) {
        return repository.findPaged(page, limit)
            .map((PageResult<Product> pageResult) -> {
                List<ProductResponse> items = pageResult.items().stream()
                    .map(mapper::toResponse)
                    .collect(Collectors.toList());
                return PagedResponse.of(items, page, limit, pageResult.total());
            });
    }
}
