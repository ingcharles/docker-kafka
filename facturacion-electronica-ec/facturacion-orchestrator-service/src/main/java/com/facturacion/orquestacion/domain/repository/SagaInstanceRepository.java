package com.facturacion.orquestacion.domain.repository;

import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.facturacion.orquestacion.domain.model.entity.SagaInstance;
import com.facturacion.orquestacion.domain.model.enums.SagaState;
import com.facturacion.orquestacion.domain.model.vo.PageResult;

public interface SagaInstanceRepository {

    Uni<SagaInstance> persist(SagaInstance instance);

    Uni<SagaInstance> findById(UUID comprobanteId);

    Uni<PageResult<SagaInstance>> findPaged(int page, int limit, SagaState state);

    /**
     * Aplica una transición de estado únicamente si la saga se encuentra
     * actualmente en uno de los estados indicados en {@code allowedFrom} —
     * compare-and-set atómico a nivel de SQL para evitar una condición de
     * carrera entre lecturas y escrituras.
     *
     * @return la saga ya actualizada, o vacío si no estaba en ninguno de
     * los estados de origen permitidos
     */
    Uni<SagaInstance> updateState(UUID comprobanteId, SagaState to, String failureReason, SagaState... allowedFrom);
}
