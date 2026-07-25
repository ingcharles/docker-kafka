package com.ecommerce.shipping.infrastructure.persistence.repository;

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

import com.ecommerce.shipping.domain.model.entity.Shipment;
import com.ecommerce.shipping.domain.model.enums.ShipmentStatus;
import com.ecommerce.shipping.domain.model.vo.PageResult;
import com.ecommerce.shipping.domain.repository.ShipmentRepository;
import com.ecommerce.shipping.infrastructure.persistence.entity.ShipmentEntity;
import com.ecommerce.shipping.infrastructure.persistence.mapper.ShipmentMapper;

@ApplicationScoped
public class ShipmentRepositoryImpl implements ShipmentRepository {

    private final ShipmentPanacheRepository repository;
    private final ShipmentMapper mapper;

    @Inject
    public ShipmentRepositoryImpl(ShipmentPanacheRepository repository, ShipmentMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Uni<Shipment> persist(Shipment domain) {
        return repository.persist(mapper.toEntity(domain)).map(mapper::toDomain);
    }

    @Override
    public Uni<Shipment> findByOrderId(UUID orderId) {
        return repository.find("orderId", orderId).firstResult()
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }

    @Override
    public Uni<PageResult<Shipment>> findPaged(int page, int limit, ShipmentStatus status) {
        String query = "1 = 1";
        Map<String, Object> params = new HashMap<>();
        if (status != null) {
            query += " and status = :status";
            params.put("status", status.name());
        }
        var panacheQuery = repository.find(query, params).page(Page.of(page, limit));
        Uni<List<ShipmentEntity>> itemsUni = panacheQuery.list();
        Uni<Long> countUni = panacheQuery.count();
        return Uni.combine().all().unis(itemsUni, countUni).asTuple()
            .map(tuple -> new PageResult<>(
                tuple.getItem1().stream().map(mapper::toDomain).collect(Collectors.toList()),
                tuple.getItem2()));
    }

    @Override
    public Uni<Shipment> updateStatus(UUID orderId, ShipmentStatus to, ShipmentStatus... allowedFrom) {
        List<String> allowedNames = new ArrayList<>();
        for (ShipmentStatus status : allowedFrom) {
            allowedNames.add(status.name());
        }
        return repository.update("status = ?1, updatedAt = ?2 where orderId = ?3 and status in ?4",
                to.name(), Instant.now(), orderId, allowedNames)
            .chain(rowsUpdated -> rowsUpdated == 0
                ? Uni.createFrom().nullItem()
                : findByOrderId(orderId));
    }
}
