package com.facturacion.orquestacion.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;

import com.facturacion.orquestacion.domain.model.entity.SagaStep;
import com.facturacion.orquestacion.domain.repository.SagaStepRepository;
import com.facturacion.orquestacion.infrastructure.persistence.mapper.SagaStepMapper;

@ApplicationScoped
public class SagaStepRepositoryImpl implements SagaStepRepository {

    private final SagaStepPanacheRepository repository;
    private final SagaStepMapper mapper;

    @Inject
    public SagaStepRepositoryImpl(SagaStepPanacheRepository repository, SagaStepMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Uni<SagaStep> persist(SagaStep domain) {
        return repository.persist(mapper.toEntity(domain)).map(mapper::toDomain);
    }

    @Override
    public Uni<List<SagaStep>> findBySagaId(UUID sagaId) {
        return repository.list("sagaId", Sort.ascending("createdAt"), sagaId)
            .map(entities -> entities.stream().map(mapper::toDomain).collect(Collectors.toList()));
    }

    @Override
    public Uni<SagaStep> findUltimaNota(UUID sagaId, String codigo) {
        return repository.find("sagaId = ?1 and status = 'NOTA' and stepName = ?2", Sort.descending("createdAt"), sagaId,
                codigo)
            .firstResult()
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }

    @Override
    public Uni<SagaStep> findLastSent(UUID sagaId) {
        return repository.find("sagaId = ?1 and status = 'SENT'", Sort.descending("createdAt"), sagaId)
            .firstResult()
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }
}
