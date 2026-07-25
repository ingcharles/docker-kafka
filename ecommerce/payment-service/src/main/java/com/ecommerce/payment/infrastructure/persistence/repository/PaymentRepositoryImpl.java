package com.ecommerce.payment.infrastructure.persistence.repository;

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

import com.ecommerce.payment.domain.model.entity.Payment;
import com.ecommerce.payment.domain.model.enums.PaymentStatus;
import com.ecommerce.payment.domain.model.vo.PageResult;
import com.ecommerce.payment.domain.repository.PaymentRepository;
import com.ecommerce.payment.infrastructure.persistence.entity.PaymentEntity;
import com.ecommerce.payment.infrastructure.persistence.mapper.PaymentMapper;

@ApplicationScoped
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentPanacheRepository repository;
    private final PaymentMapper mapper;

    @Inject
    public PaymentRepositoryImpl(PaymentPanacheRepository repository, PaymentMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Uni<Payment> persist(Payment domain) {
        return repository.persist(mapper.toEntity(domain)).map(mapper::toDomain);
    }

    @Override
    public Uni<Payment> findByOrderId(UUID orderId) {
        return repository.find("orderId", orderId).firstResult()
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }

    @Override
    public Uni<PageResult<Payment>> findPaged(int page, int limit, PaymentStatus status) {
        String query = "1 = 1";
        Map<String, Object> params = new HashMap<>();
        if (status != null) {
            query += " and status = :status";
            params.put("status", status.name());
        }
        var panacheQuery = repository.find(query, params).page(Page.of(page, limit));
        Uni<List<PaymentEntity>> itemsUni = panacheQuery.list();
        Uni<Long> countUni = panacheQuery.count();
        return Uni.combine().all().unis(itemsUni, countUni).asTuple()
            .map(tuple -> new PageResult<>(
                tuple.getItem1().stream().map(mapper::toDomain).collect(Collectors.toList()),
                tuple.getItem2()));
    }

    @Override
    public Uni<Payment> updateStatus(UUID orderId, PaymentStatus to, String gatewayReference,
        String declineReason, PaymentStatus... allowedFrom) {
        List<String> allowedNames = new ArrayList<>();
        for (PaymentStatus status : allowedFrom) {
            allowedNames.add(status.name());
        }
        return repository.update(
                "status = ?1, gatewayReference = coalesce(?2, gatewayReference), "
                    + "declineReason = coalesce(?3, declineReason), updatedAt = ?4 "
                    + "where orderId = ?5 and status in ?6",
                to.name(), gatewayReference, declineReason, Instant.now(), orderId, allowedNames)
            .chain(rowsUpdated -> rowsUpdated == 0
                ? Uni.createFrom().nullItem()
                : findByOrderId(orderId));
    }
}
