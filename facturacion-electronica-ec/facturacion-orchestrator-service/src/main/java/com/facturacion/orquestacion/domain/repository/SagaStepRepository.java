package com.facturacion.orquestacion.domain.repository;

import java.util.List;
import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.facturacion.orquestacion.domain.model.entity.SagaStep;

public interface SagaStepRepository {

    Uni<SagaStep> persist(SagaStep step);

    Uni<List<SagaStep>> findBySagaId(UUID sagaId);

    /** El paso SENT más reciente — es lo que {@code POST /sagas/{comprobanteId}/retry} vuelve a publicar. */
    Uni<SagaStep> findLastSent(UUID sagaId);

    /** La nota más reciente con ese código (p. ej. el comando de firma que queda esperando la reserva de stock). */
    Uni<SagaStep> findUltimaNota(UUID sagaId, String codigo);
}
