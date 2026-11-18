package com.facturacion.commons.reactive;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * Corre trabajo bloqueante o intensivo en CPU (bcrypt, firma XML, parseo SOAP, PDF, leer un archivo subido) fuera del
 * event loop y vuelve al contexto de Vert.x del llamador antes de seguir. El regreso al contexto original NO es
 * opcional: Hibernate Reactive (y el cierre de {@code @WithSession}/{@code @WithTransaction}) debe continuar en el
 * mismo contexto en que se abrió la sesión, no en un hilo del worker pool.
 * <p>
 * Es el único lugar con este patrón: ningún servicio arma a mano {@code runSubscriptionOn} + {@code emitOn}.
 * <p>
 * Debe invocarse en el momento de armar el {@code Uni} (dentro de un {@code transformToUni}, ya en el contexto de
 * Vert.x), no antes. Sin contexto de Vert.x (p. ej. en un test unitario o en un job programado) simplemente emite en
 * el hilo del ejecutor.
 */
public final class TrabajoBloqueante {

    private TrabajoBloqueante() {
    }

    /** En el worker pool de Quarkus (compartido por todo el servicio). */
    public static <T> Uni<T> ejecutar(Supplier<T> trabajo) {
        return ejecutar(trabajo, Infrastructure.getDefaultWorkerPool());
    }

    /**
     * En un ejecutor propio y acotado: para trabajo pesado y expuesto a ráfagas (bcrypt en el login) que no debe
     * agotar el worker pool compartido. Si el ejecutor rechaza la tarea (cola llena) el {@code Uni} falla con la
     * excepción del ejecutor.
     */
    public static <T> Uni<T> ejecutar(Supplier<T> trabajo, Executor ejecutor) {
        Context contexto = Vertx.currentContext();
        Uni<T> uni = Uni.createFrom().item(trabajo).runSubscriptionOn(ejecutor);
        return contexto == null ? uni : uni.emitOn(tarea -> contexto.runOnContext(ignorado -> tarea.run()));
    }
}
