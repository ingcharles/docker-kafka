package com.facturacion.orquestacion.infrastructure.persistence.repository;

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
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;

import com.facturacion.commons.paging.PageResult;
import com.facturacion.orquestacion.domain.model.entity.SagaInstance;
import com.facturacion.orquestacion.domain.model.enums.SagaState;
import com.facturacion.orquestacion.domain.repository.SagaInstanceRepository;
import com.facturacion.orquestacion.infrastructure.persistence.entity.SagaInstanceEntity;
import com.facturacion.orquestacion.infrastructure.persistence.mapper.SagaInstanceMapper;

@ApplicationScoped
public class SagaInstanceRepositoryImpl implements SagaInstanceRepository {

    private final SagaInstancePanacheRepository repository;
    private final SagaInstanceMapper mapper;

    @Inject
    public SagaInstanceRepositoryImpl(SagaInstancePanacheRepository repository, SagaInstanceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Uni<SagaInstance> persist(SagaInstance domain) {
        return repository.persist(mapper.toEntity(domain)).map(mapper::toDomain);
    }

    @Override
    public Uni<SagaInstance> findById(UUID comprobanteId) {
        return repository.findById(comprobanteId).map(entity -> entity == null ? null : mapper.toDomain(entity));
    }

    @Override
    public Uni<PageResult<SagaInstance>> findPaged(int page, int limit, SagaState state, String empresaRuc) {
        String query = "empresaRuc = :empresaRuc";
        Map<String, Object> params = new HashMap<>();
        params.put("empresaRuc", empresaRuc);
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
    public Uni<SagaInstance> transicionarEstado(UUID comprobanteId, SagaState to, String failureReason,
            SagaState... allowedFrom) {
        List<String> allowedNames = new ArrayList<>();
        for (SagaState state : allowedFrom) {
            allowedNames.add(state.name());
        }
        return repository.update(
                "state = ?1, failureReason = coalesce(?2, failureReason), updatedAt = ?3 "
                    + "where id = ?4 and state in ?5",
                to.name(), failureReason, Instant.now(), comprobanteId, allowedNames)
            .chain(rowsUpdated -> rowsUpdated == 0
                ? Uni.createFrom().nullItem()
                : findById(comprobanteId));
    }

    @Override
    public Uni<List<SagaInstance>> findByEstados(List<SagaState> estados, int limite) {
        List<String> nombres = estados.stream().map(SagaState::name).collect(Collectors.toList());
        return repository.find("state in ?1", Sort.ascending("updatedAt"), nombres).page(Page.ofSize(limite)).list()
            .map(entidades -> entidades.stream().map(mapper::toDomain).collect(Collectors.toList()));
    }

    @Override
    public Uni<SagaInstance> registrarReintento(UUID comprobanteId, SagaState estadoActual) {
        return repository.update("reintentos = reintentos + 1, updatedAt = ?1 where id = ?2 and state = ?3",
                Instant.now(), comprobanteId, estadoActual.name())
            .chain(filas -> filas == 0 ? Uni.createFrom().nullItem() : findById(comprobanteId));
    }
}
