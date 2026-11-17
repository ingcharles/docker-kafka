package com.facturacion.orquestacion.domain.service;

import java.time.Duration;
import java.time.Instant;

import com.facturacion.orquestacion.domain.model.entity.SagaInstance;
import com.facturacion.orquestacion.domain.model.enums.SagaState;

/**
 * Decide qué hace el job de reenvío de correos con una saga. Es lógica pura (sin base de datos ni reloj propio): el job
 * solo la consulta y ejecuta la acción.
 * <ul>
 *   <li>{@code NOTIFICACION_FALLIDA} con intentos disponibles y quieta desde hace {@code esperaMinima} →
 *       {@link Accion#REENVIAR} (cuenta un intento);</li>
 *   <li>{@code AUTORIZADO} que espera una notificación y no recibió respuesta en {@code plazoRespuesta} →
 *       {@link Accion#DAR_POR_SIN_RESPUESTA} (vuelve a NOTIFICACION_FALLIDA, sin contar intento; así el flujo nunca
 *       queda colgado esperando un resultado que se perdió);</li>
 *   <li>cualquier otro caso, o antes de cumplirse el plazo → {@link Accion#ESPERAR}.</li>
 * </ul>
 * Con los intentos agotados la saga queda en NOTIFICACION_FALLIDA y el job ya no la toca: solo un reenvío manual por
 * REST (que no tiene tope) la saca de ahí.
 */
public final class PoliticaReenvioNotificacion {

    public enum Accion { ESPERAR, REENVIAR, DAR_POR_SIN_RESPUESTA }

    /**
     * @param esperaMinima    cuánto debe llevar quieta una saga fallida antes de reenviarla otra vez
     * @param plazoRespuesta  cuánto esperar el resultado del envío antes de darlo por perdido
     * @param maxIntentos     reenvíos automáticos por saga
     */
    public record Plazos(Duration esperaMinima, Duration plazoRespuesta, int maxIntentos) {
    }

    private PoliticaReenvioNotificacion() {
    }

    public static Accion decidir(SagaInstance saga, Instant ahora, Plazos plazos) {
        Instant ultimaActividad = saga.getUpdatedAt();
        if (saga.getState() == SagaState.NOTIFICACION_FALLIDA) {
            boolean quedanIntentos = saga.getReintentosNotificacion() < plazos.maxIntentos();
            boolean esperoLoSuficiente = !ultimaActividad.plus(plazos.esperaMinima()).isAfter(ahora);
            return quedanIntentos && esperoLoSuficiente ? Accion.REENVIAR : Accion.ESPERAR;
        }
        if (saga.getState() == SagaState.AUTORIZADO && saga.isNotificacionEsperada()
                && !ultimaActividad.plus(plazos.plazoRespuesta()).isAfter(ahora)) {
            return Accion.DAR_POR_SIN_RESPUESTA;
        }
        return Accion.ESPERAR;
    }
}
