package com.ecommerce.order.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Uni;

import com.ecommerce.order.domain.model.entity.Order;
import com.ecommerce.order.domain.model.enums.OrderStatus;
import com.ecommerce.order.domain.model.vo.PageResult;
import com.ecommerce.order.domain.repository.OrderRepository;
import com.ecommerce.order.infrastructure.persistence.entity.OrderEntity;
import com.ecommerce.order.infrastructure.persistence.mapper.OrderMapper;

@ApplicationScoped
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderPanacheRepository repository;
    private final OrderMapper mapper;

    @Inject
    public OrderRepositoryImpl(OrderPanacheRepository repository, OrderMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Uni<Order> persist(Order domain) {
        return repository.persist(mapper.toEntity(domain)).map(mapper::toDomain);
    }

    @Override
    public Uni<Order> findById(UUID id) {
        return repository.find("id = ?1", id)
            .firstResult()
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }

    @Override
    public Uni<PageResult<Order>> findPaged(int page, int limit, String customerId, OrderStatus status) {
        StringBuilder query = new StringBuilder("1 = 1");
        Map<String, Object> params = new HashMap<>();
        if (customerId != null && !customerId.isBlank()) {
            query.append(" and customerId = :customerId");
            params.put("customerId", customerId);
        }
        if (status != null) {
            query.append(" and status = :status");
            params.put("status", status.name());
        }

        var panacheQuery = repository.find(query.toString(), params).page(Page.of(page, limit));
        Uni<List<OrderEntity>> itemsUni = panacheQuery.list();
        Uni<Long> countUni = panacheQuery.count();

        return Uni.combine().all().unis(itemsUni, countUni)
            .asTuple()
            .map(tuple -> new PageResult<>(
                tuple.getItem1().stream().map(mapper::toDomain).collect(Collectors.toList()),
                tuple.getItem2()));
    }

    @Override
    public Uni<Order> updateStatus(UUID id, OrderStatus to, OrderStatus... allowedFrom) {
        List<String> allowedNames = new ArrayList<>();
        for (OrderStatus status : allowedFrom) {
            allowedNames.add(status.name());
        }
        return repository.update("status = ?1, updatedAt = ?2 where id = ?3 and status in ?4",
                to.name(), Instant.now(), id, allowedNames)
            .chain(rowsUpdated -> rowsUpdated == 0
                ? Uni.createFrom().nullItem()
                : findById(id));
    }
}
