package com.facturacion.commons.reactive;

import java.util.function.Supplier;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * Corre trabajo bloqueante o intensivo en CPU (bcrypt, firma XML, parseo SOAP, PDF) fuera del event loop y vuelve al
 * contexto de Vert.x del llamador antes de seguir. El regreso al contexto original NO es opcional: Hibernate
 * Reactive (y el cierre de {@code @WithSession}/{@code @WithTransaction}) debe continuar en el mismo contexto en que
 * se abrió la sesión, no en un hilo del worker pool.
 * <p>
 * Debe invocarse en el momento de armar el {@code Uni} (dentro de un {@code transformToUni}, ya en el contexto de
 * Vert.x), no antes. Sin contexto de Vert.x (p. ej. en un test unitario) simplemente emite en el worker pool.
 */
public final class TrabajoBloqueante {

    private TrabajoBloqueante() {
    }

    public static <T> Uni<T> ejecutar(Supplier<T> trabajo) {
        Context contexto = Vertx.currentContext();
        Uni<T> uni = Uni.createFrom().item(trabajo).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
        return contexto == null ? uni : uni.emitOn(tarea -> contexto.runOnContext(ignorado -> tarea.run()));
    }
}
