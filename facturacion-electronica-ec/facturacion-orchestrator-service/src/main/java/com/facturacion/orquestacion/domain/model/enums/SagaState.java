package com.facturacion.orquestacion.domain.model.enums;

import java.util.Arrays;
import java.util.List;

/**
 * Ciclo de vida de la saga de un comprobante. Casi no hay nada que compensar (solo el stock reservado): firmar, enviar al SRI y notificar son pasos que o bien no tienen
 * efecto colateral que deshacer (firmar, notificar) o ya son definitivos por naturaleza fiscal (una vez que el
 * SRI autoriza, el comprobante es legalmente válido pase lo que pase después) — así que
 * FIRMA_FALLIDA/RECHAZADO_RECEPCION/NO_AUTORIZADO son estados terminales de error sin ningún comando de
 * compensación, y NOTIFICACION_FALLIDA es un terminal de error que ni siquiera toca el estado de
 * comprobantes-service (el comprobante ya está AUTORIZADO, sigue siéndolo aunque el correo falle).
 * <p>
 * Cada estado declara dos propiedades que usa el watchdog (ver {@code SagaWatchdog}):
 * <ul>
 *   <li>{@code terminal}: la saga ya no se mueve;</li>
 *   <li>{@code reintentable}: la saga está esperando la respuesta de otro servicio y, si no llega, tiene sentido
 *       volver a publicar el último comando (idempotente en el destino). {@link #AUTORIZADO} espera la
 *       notificación pero NO es reintentable: su último comando ({@code MARCAR_AUTORIZADO}) ya se cumplió y la
 *       notificación solo existe si el comprobante tiene correo.</li>
 * </ul>
 */
public enum SagaState {

    /** Con descuento de inventario: se pidió reservar el stock y se espera la respuesta de inventario-service. */
    STOCK_PENDIENTE(false, true),
    /** Stock reservado: se pidió la firma y se espera su respuesta (equivale a CREADO en el flujo sin inventario). */
    STOCK_RESERVADO(false, true),
    /** Inventario no pudo reservar el stock de alguna línea: terminal de error (inventario ya compensó lo parcial). */
    STOCK_NO_DISPONIBLE(true, false),
    CREADO(false, true),
    FIRMADO(false, true),
    FIRMA_FALLIDA(true, false),
    RECHAZADO_RECEPCION(true, false),
    AUTORIZADO(false, false),
    /** Autorizada por el SRI y sin nada más que esperar: el comprobante no tiene correo, así que no habrá notificación. */
    AUTORIZADO_SIN_NOTIFICACION(true, false),
    NO_AUTORIZADO(true, false),
    NOTIFICADO(true, false),
    /** El correo falló; el comprobante sigue autorizado. Solo se sale con un reenvío exitoso del correo (-> NOTIFICADO). */
    NOTIFICACION_FALLIDA(true, false),
    /** El watchdog agotó los reintentos sin recibir respuesta: requiere atención humana. */
    EXPIRADA(true, false);

    private final boolean terminal;
    private final boolean reintentable;

    SagaState(boolean terminal, boolean reintentable) {
        this.terminal = terminal;
        this.reintentable = reintentable;
    }

    public boolean esTerminal() {
        return terminal;
    }

    public boolean esReintentable() {
        return reintentable;
    }

    public static List<SagaState> reintentables() {
        return Arrays.stream(values()).filter(SagaState::esReintentable).toList();
    }
}
