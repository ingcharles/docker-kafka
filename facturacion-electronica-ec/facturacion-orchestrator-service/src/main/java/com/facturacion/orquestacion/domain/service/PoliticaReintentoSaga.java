package com.facturacion.orquestacion.domain.service;

import java.time.Duration;
import java.time.Instant;

import com.facturacion.orquestacion.domain.model.entity.SagaInstance;
import com.facturacion.orquestacion.domain.model.enums.SagaState;

/**
 * Decide qué hacer con una saga que podría estar atascada. Es lógica pura (sin base de datos ni reloj propio) para
 * poder probarla: el watchdog solo la consulta y ejecuta la acción.
 * <p>
 * Una saga {@linkplain SagaState#esReintentable() reintentable} espera la respuesta de otro servicio. Si pasa el
 * plazo de su estado desde la última actividad ({@code updatedAt}, que el reintento también renueva):
 * <ul>
 *   <li>con reintentos disponibles → {@link Accion#REINTENTAR} (volver a publicar el último comando);</li>
 *   <li>con los reintentos agotados → {@link Accion#EXPIRAR} (estado terminal EXPIRADA, para que un humano la
 *       revise);</li>
 *   <li>antes del plazo, o si el estado no es reintentable → {@link Accion#ESPERAR}.</li>
 * </ul>
 */
public final class PoliticaReintentoSaga {

    public enum Accion { ESPERAR, REINTENTAR, EXPIRAR }

    /**
     * @param plazoFirma     cuánto esperar la respuesta de firma-electronica-service (estado CREADO)
     * @param plazoSri       cuánto esperar la respuesta del SRI (estado FIRMADO; el SRI puede tardar más)
     * @param maxReintentos  reintentos automáticos antes de expirar
     */
    public record Plazos(Duration plazoFirma, Duration plazoSri, int maxReintentos) {
    }

    private PoliticaReintentoSaga() {
    }

    public static Accion decidir(SagaInstance saga, Instant ahora, Plazos plazos) {
        Duration plazo = plazoDe(saga.getState(), plazos);
        if (plazo == null || saga.getUpdatedAt().plus(plazo).isAfter(ahora)) {
            return Accion.ESPERAR;
        }
        return saga.getReintentos() >= plazos.maxReintentos() ? Accion.EXPIRAR : Accion.REINTENTAR;
    }

    /** Plazo de espera del estado, o {@code null} si el estado no se reintenta. */
    private static Duration plazoDe(SagaState estado, Plazos plazos) {
        if (!estado.esReintentable()) {
            return null;
        }
        return switch (estado) {
            case CREADO -> plazos.plazoFirma();
            case FIRMADO -> plazos.plazoSri();
            default -> null;
        };
    }
}
