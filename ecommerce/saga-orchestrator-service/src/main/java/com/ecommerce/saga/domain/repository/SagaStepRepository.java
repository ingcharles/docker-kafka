package com.ecommerce.saga.domain.repository;

import java.util.List;
import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.ecommerce.saga.domain.model.entity.SagaStep;

/**
 * Puerto (interfaz) para la persistencia de SagaStep, siguiendo arquitectura
 * hexagonal. La implementación concreta (el adaptador) vive en
 * {@code infrastructure.persistence}, usando Panache.
 */
public interface SagaStepRepository {

    Uni<SagaStep> persist(SagaStep step);

    Uni<List<SagaStep>> findBySagaId(UUID sagaId);

    /** El paso SENT más reciente — es lo que {@code POST /sagas/{orderId}/retry} vuelve a publicar. */
    Uni<SagaStep> findLastSent(UUID sagaId);
}
