package com.facturacion.orquestacion.domain.repository;

import java.util.List;
import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.facturacion.commons.paging.PageResult;
import com.facturacion.orquestacion.domain.model.entity.SagaInstance;
import com.facturacion.orquestacion.domain.model.enums.SagaState;

public interface SagaInstanceRepository {

    Uni<SagaInstance> persist(SagaInstance instance);

    Uni<SagaInstance> findById(UUID comprobanteId);

    Uni<PageResult<SagaInstance>> findPaged(int page, int limit, SagaState state, String empresaRuc);

    /**
     * Aplica una transición de estado únicamente si la saga se encuentra
     * actualmente en uno de los estados indicados en {@code allowedFrom} —
     * compare-and-set atómico a nivel de SQL para evitar una condición de
     * carrera entre lecturas y escrituras.
     *
     * @return la saga ya actualizada, o vacío si no estaba en ninguno de
     * los estados de origen permitidos
     */
    Uni<SagaInstance> transicionarEstado(UUID comprobanteId, SagaState to, String failureReason, SagaState... allowedFrom);

    /** Sagas en alguno de esos estados, las que llevan más tiempo sin actividad primero (para el watchdog). */
    Uni<List<SagaInstance>> findByEstados(List<SagaState> estados, int limite);

    /**
     * Cuenta un reintento automático: suma 1 a {@code reintentos} y renueva {@code updatedAt}, solo si la saga
     * sigue en {@code estadoActual} (compare-and-set: si otro evento la movió mientras tanto, no hace nada).
     *
     * @return la saga actualizada, o vacío si ya no estaba en ese estado
     */
    Uni<SagaInstance> registrarReintento(UUID comprobanteId, SagaState estadoActual);}
