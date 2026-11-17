package com.facturacion.orquestacion.domain.repository;

import java.time.Instant;
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

    /**
     * Igual que {@link #transicionarEstado}, pero borra el motivo de un fallo anterior: para cuando la saga se recupera
     * (p. ej. un correo que falló y se reenvió bien).
     */
    Uni<SagaInstance> transicionarEstadoLimpiandoMotivo(UUID comprobanteId, SagaState to, SagaState... allowedFrom);

    /** Sagas en alguno de esos estados, las que llevan más tiempo sin actividad primero (para el watchdog). */
    Uni<List<SagaInstance>> findByEstados(List<SagaState> estados, int limite);

    /**
     * Cuenta un reintento automático: suma 1 a {@code reintentos} y renueva {@code updatedAt}, solo si la saga
     * sigue en {@code estadoActual} (compare-and-set: si otro evento la movió mientras tanto, no hace nada).
     *
     * @return la saga actualizada, o vacío si ya no estaba en ese estado
     */
    Uni<SagaInstance> registrarReintento(UUID comprobanteId, SagaState estadoActual);

    /**
     * Sagas que el job de reenvío de correos debe mirar, las más antiguas primero y como máximo {@code limite}: las
     * NOTIFICACION_FALLIDA con menos de {@code maxIntentos} reenvíos automáticos y sin actividad desde
     * {@code fallidasAntesDe}, y las AUTORIZADO que esperan una notificación y no tienen actividad desde
     * {@code sinRespuestaAntesDe}. El filtro va en SQL a propósito: las que ya agotaron sus intentos no deben ocupar
     * lugar en el lote.
     */
    Uni<List<SagaInstance>> findPendientesDeNotificacion(Instant fallidasAntesDe, Instant sinRespuestaAntesDe,
            int maxIntentos, int limite);

    /**
     * Cuenta un reenvío automático y devuelve la saga a AUTORIZADO (a la espera del resultado) en una sola sentencia,
     * solo si sigue en NOTIFICACION_FALLIDA y le quedan intentos: compare-and-set, así que dos instancias del job (o el
     * job y un reenvío manual) no la reenvían dos veces.
     *
     * @return la saga, o vacío si ya no estaba en NOTIFICACION_FALLIDA o ya no tenía intentos
     */
    Uni<SagaInstance> registrarReenvioAutomatico(UUID comprobanteId, int maxIntentos);
}
