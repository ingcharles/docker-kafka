package com.facturacion.commons.limite;

import java.time.Duration;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowIterator;
import io.vertx.mutiny.sqlclient.Tuple;

/**
 * Lee el límite de la tabla {@code limite_configuracion} del esquema del servicio (la crea su migración Flyway). Se lee en
 * cada petición limitada: un cambio en la tabla rige de inmediato en todas las réplicas.
 */
@ApplicationScoped
public class ResolutorDeLimitesPostgres implements ResolutorDeLimites {

    /** La fila de la empresa va antes que la general ({@code false} ordena primero). */
    private static final String BUSCAR = """
        SELECT maximo_usuario, maximo_empresa, ventana_segundos FROM limite_configuracion
        WHERE operacion = $1 AND ruc IN ($2, $3)
        ORDER BY ruc = $3
        LIMIT 1
        """;

    private static final String COLUMNA_MAXIMO_USUARIO = "maximo_usuario";
    private static final String COLUMNA_MAXIMO_EMPRESA = "maximo_empresa";
    private static final String COLUMNA_VENTANA = "ventana_segundos";

    private final Instance<Pool> pool;

    @Inject
    public ResolutorDeLimitesPostgres(Instance<Pool> pool) {
        this.pool = pool;
    }

    @Override
    public Uni<Optional<Limite>> resolver(OperacionLimitada operacion, String ruc) {
        String empresa = ruc == null ? LimitadorDeUso.CUALQUIER_EMPRESA : ruc;
        return pool.get().preparedQuery(BUSCAR)
            .execute(Tuple.of(operacion.nombre(), empresa, LimitadorDeUso.CUALQUIER_EMPRESA))
            .map(filas -> {
                RowIterator<Row> resultado = filas.iterator();
                if (!resultado.hasNext()) {
                    return Optional.empty();
                }
                Row fila = resultado.next();
                return Optional.of(new Limite(fila.getInteger(COLUMNA_MAXIMO_USUARIO),
                    fila.getInteger(COLUMNA_MAXIMO_EMPRESA), Duration.ofSeconds(fila.getInteger(COLUMNA_VENTANA))));
            });
    }
}
