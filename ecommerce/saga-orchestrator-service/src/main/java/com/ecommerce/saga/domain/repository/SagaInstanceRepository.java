package com.ecommerce.saga.domain.repository;

import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.ecommerce.saga.domain.model.entity.SagaInstance;
import com.ecommerce.saga.domain.model.enums.SagaState;
import com.ecommerce.saga.domain.model.vo.PageResult;

/**
 * Puerto (interfaz) para la persistencia de SagaInstance, siguiendo
 * arquitectura hexagonal. La implementación concreta (el adaptador) vive en
 * {@code infrastructure.persistence}, usando Panache.
 */
public interface SagaInstanceRepository {

    Uni<SagaInstance> persist(SagaInstance instance);

    Uni<SagaInstance> findById(UUID orderId);

    Uni<PageResult<SagaInstance>> findPaged(int page, int limit, SagaState state);

    /**
     * Aplica una transición de estado únicamente si la saga se encuentra
     * actualmente en uno de los estados indicados en {@code allowedFrom} —
     * el mismo patrón de "compare-and-set" atómico a nivel de SQL que usa
     * {@code OrderRepository.updateStatus} en order-service, por la misma
     * razón: evitar una condición de carrera entre lecturas y escrituras.
     *
     * @return la saga ya actualizada, o vacío si no estaba en ninguno de
     * los estados de origen permitidos
     */
    Uni<SagaInstance> updateState(UUID orderId, SagaState to, String failureReason,
        SagaState... allowedFrom);
}
