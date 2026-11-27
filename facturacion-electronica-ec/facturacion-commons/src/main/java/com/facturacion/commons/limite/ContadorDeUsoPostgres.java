package com.facturacion.commons.limite;

import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;

/**
 * Contador de ventana fija en la tabla {@code limite_uso} del esquema del servicio (la crea su migración Flyway). Va con
 * una sentencia suelta sobre el pool, fuera de la sesión de Hibernate: lo consumido queda aunque la operación de negocio
 * falle o haga rollback. El {@link Instance} evita exigir un pool a quien no limita ninguna operación.
 */
@ApplicationScoped
public class ContadorDeUsoPostgres implements ContadorDeUso {

    private static final String CONSUMIR = """
        INSERT INTO limite_uso AS l (clave, inicio_ventana, usos) VALUES ($1, now(), 1)
        ON CONFLICT (clave) DO UPDATE SET
            inicio_ventana = CASE WHEN l.inicio_ventana <= now() - make_interval(secs => $2)
                THEN now() ELSE l.inicio_ventana END,
            usos = CASE WHEN l.inicio_ventana <= now() - make_interval(secs => $2) THEN 1 ELSE l.usos + 1 END
        RETURNING usos,
            ceil(extract(epoch FROM inicio_ventana + make_interval(secs => $2) - now()))::bigint AS restantes
        """;

    /** Borra además las filas viejas: ninguna ventana dura más de un día. */
    private static final String REINICIAR =
        "DELETE FROM limite_uso WHERE clave = $1 OR inicio_ventana < now() - interval '1 day'";

    private static final String COLUMNA_USOS = "usos";
    private static final String COLUMNA_RESTANTES = "restantes";

    private final Instance<Pool> pool;

    @Inject
    public ContadorDeUsoPostgres(Instance<Pool> pool) {
        this.pool = pool;
    }

    @Override
    public Uni<Uso> consumir(String clave, Duration ventana) {
        return pool.get().preparedQuery(CONSUMIR).execute(Tuple.of(clave, (double) ventana.toSeconds()))
            .map(filas -> {
                Row fila = filas.iterator().next();
                return new Uso(fila.getInteger(COLUMNA_USOS), fila.getLong(COLUMNA_RESTANTES));
            });
    }

    @Override
    public Uni<Void> reiniciar(String clave) {
        return pool.get().preparedQuery(REINICIAR).execute(Tuple.of(clave)).replaceWithVoid();
    }
}
