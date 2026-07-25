package com.ecommerce.saga.infrastructure.persistence.repository;

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

import com.ecommerce.saga.domain.model.entity.SagaInstance;
import com.ecommerce.saga.domain.model.enums.SagaState;
import com.ecommerce.saga.domain.model.vo.PageResult;
import com.ecommerce.saga.domain.repository.SagaInstanceRepository;
import com.ecommerce.saga.infrastructure.persistence.entity.SagaInstanceEntity;
import com.ecommerce.saga.infrastructure.persistence.mapper.SagaInstanceMapper;

@ApplicationScoped
public class SagaInstanceRepositoryImpl implements SagaInstanceRepository {

    private final SagaInstancePanacheRepository repository;
    private final SagaInstanceMapper mapper;

    @Inject
    public SagaInstanceRepositoryImpl(SagaInstancePanacheRepository repository,
        SagaInstanceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Uni<SagaInstance> persist(SagaInstance domain) {
        return repository.persist(mapper.toEntity(domain)).map(mapper::toDomain);
    }

    @Override
    public Uni<SagaInstance> findById(UUID orderId) {
        return repository.findById(orderId).map(entity -> entity == null ? null : mapper.toDomain(entity));
    }

    @Override
    public Uni<PageResult<SagaInstance>> findPaged(int page, int limit, SagaState state) {
        String query = "1 = 1";
        Map<String, Object> params = new HashMap<>();
        if (state != null) {
            query += " and state = :state";
            params.put("state", state.name());
        }
        var panacheQuery = repository.find(query, params).page(Page.of(page, limit));
        Uni<List<SagaInstanceEntity>> itemsUni = panacheQuery.list();
        Uni<Long> countUni = panacheQuery.count();
        return Uni.combine().all().unis(itemsUni, countUni).asTuple()
            .map(tuple -> new PageResult<>(
                tuple.getItem1().stream().map(mapper::toDomain).collect(Collectors.toList()),
                tuple.getItem2()));
    }

    @Override
    public Uni<SagaInstance> updateState(UUID orderId, SagaState to, String failureReason,
        SagaState... allowedFrom) {
        List<String> allowedNames = new ArrayList<>();
        for (SagaState state : allowedFrom) {
            allowedNames.add(state.name());
        }
        return repository.update(
                "state = ?1, failureReason = coalesce(?2, failureReason), updatedAt = ?3 "
                    + "where id = ?4 and state in ?5",
                to.name(), failureReason, Instant.now(), orderId, allowedNames)
            .chain(rowsUpdated -> rowsUpdated == 0
                ? Uni.createFrom().nullItem()
                : findById(orderId));
    }
}
