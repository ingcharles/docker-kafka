package com.facturacion.comprobantes.application.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;
import io.vertx.pgclient.PgException;

import com.facturacion.comprobantes.domain.catalogo.CatalogoDef;
import com.facturacion.comprobantes.domain.catalogo.CatalogoDef.Columna;
import com.facturacion.comprobantes.domain.catalogo.CatalogoDef.Tipo;
import com.facturacion.comprobantes.shared.error.CodigoError;
import com.facturacion.commons.exception.ErrorNegocioException;

/**
 * Consulta y mantenimiento de los catálogos del SRI (formas de pago, impuestos y tarifas, tipos de identificación,
 * retenciones, sustento). Son tablas de este servicio: se siembran con la ficha técnica en la migración V14 y se
 * modifican con la API {@code /catalogos} sin redesplegar (p. ej. cuando el SRI cambia una tarifa).
 * <p>
 * Todos los catálogos tienen la misma forma de operación, así que en vez de una entidad + recurso REST por
 * catálogo se describen una vez en {@link CatalogoDef} y aquí se tratan de forma genérica. Todo el SQL se arma a
 * partir de esa descripción (nombres de tabla y columna salen de {@code CatalogoDef.TODOS}, nunca del request, así
 * que no hay inyección). No hay caché: la siguiente emisión lee la tabla
 * ya modificada (ver {@code CatalogoSriRepositoryImpl}).
 */
@ApplicationScoped
public class CatalogoAdminService {

    /** SQLSTATE de Postgres que indican un dato inválido del caller (FK, NOT NULL, CHECK), no un fallo del servidor. */
    private static final String SQLSTATE_NOT_NULL = "23502";
    private static final String SQLSTATE_FOREIGN_KEY = "23503";
    private static final String SQLSTATE_CHECK = "23514";
    private static final Set<String> SQLSTATE_DATO_INVALIDO =
        Set.of(SQLSTATE_NOT_NULL, SQLSTATE_FOREIGN_KEY, SQLSTATE_CHECK);

    private final PgPool pool;

    @Inject
    public CatalogoAdminService(PgPool pool) {
        this.pool = pool;
    }

    /** @param soloVigentes si es true: activo y vigente en {@code fecha} (hoy si es null). */
    public Uni<List<Map<String, Object>>> listar(CatalogoDef def, boolean soloVigentes, LocalDate fecha) {
        StringBuilder sql = new StringBuilder("select ").append(columnasSelect(def)).append(" from ")
            .append(def.nombre());
        Tuple params = Tuple.tuple();
        if (soloVigentes) {
            sql.append(" where activo = true");
            if (def.tieneVigencia()) {
                sql.append(" and vigente_desde <= $1::text::date and (vigente_hasta is null or vigente_hasta >= $1::text::date)");
                params.addString((fecha == null ? LocalDate.now() : fecha).toString());
            }
        }
        sql.append(" order by ").append(def.claves().stream().map(Columna::nombre).collect(Collectors.joining(", ")));
        Uni<io.vertx.mutiny.sqlclient.RowSet<Row>> consulta = params.size() == 0
            ? pool.query(sql.toString()).execute()
            : pool.preparedQuery(sql.toString()).execute(params);
        return consulta.map(filas -> {
            List<Map<String, Object>> resultado = new ArrayList<>();
            filas.forEach(fila -> resultado.add(aMapa(def, fila)));
            return resultado;
        });
    }

    /** Valida, hace upsert por clave y devuelve la fila tal como quedó guardada. */
    public Uni<Map<String, Object>> guardar(CatalogoDef def, Map<String, Object> body) {
        Map<String, Object> valores = def.validar(body);
        Tuple params = Tuple.tuple();
        for (Columna c : def.columnas()) {
            Object valor = valores.get(c.nombre());
            if (c.tipo() == Tipo.BOOLEANO) {
                params.addBoolean((Boolean) valor);
            } else {
                params.addString((String) valor);
            }
        }
        return pool.preparedQuery(upsertSql(def)).execute(params)
            .map(filas -> aMapa(def, filas.iterator().next()))
            .onFailure(PgException.class).transform(CatalogoAdminService::traducir);
    }

    /**
     * Upsert de una fila por su clave. Los DECIMAL y FECHA viajan como texto (body JSON): el parámetro se declara
     * {@code ::text} y de ahí se convierte. Con un simple {@code $n::numeric} el cliente de Postgres esperaría un
     * tipo numérico y rechazaría el String. Estático y sin estado para poder probar el SQL generado.
     */
    static String upsertSql(CatalogoDef def) {
        List<Columna> columnas = def.columnas();
        String insertCols = columnas.stream().map(Columna::nombre).collect(Collectors.joining(", "));
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < columnas.size(); i++) {
            if (i > 0) {
                placeholders.append(", ");
            }
            placeholders.append('$').append(i + 1).append(switch (columnas.get(i).tipo()) {
                case DECIMAL -> "::text::numeric";
                case FECHA -> "::text::date";
                default -> "";
            });
        }
        String claves = def.claves().stream().map(Columna::nombre).collect(Collectors.joining(", "));
        String actualizar = columnas.stream().filter(c -> !c.clave())
            .map(c -> c.nombre() + " = EXCLUDED." + c.nombre()).collect(Collectors.joining(", "));
        return "insert into " + def.nombre() + " (" + insertCols + ") values (" + placeholders
            + ") on conflict (" + claves + ") do update set " + actualizar + ", actualizado_en = now()"
            + " returning " + columnasSelect(def);
    }

    private static String columnasSelect(CatalogoDef def) {
        return def.columnas().stream().map(c -> switch (c.tipo()) {
            case DECIMAL, FECHA -> c.nombre() + "::text as " + c.nombre();
            default -> c.nombre();
        }).collect(Collectors.joining(", "));
    }

    private static Map<String, Object> aMapa(CatalogoDef def, Row fila) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        for (Columna c : def.columnas()) {
            mapa.put(c.nombre(), c.tipo() == Tipo.BOOLEANO ? fila.getBoolean(c.nombre()) : fila.getString(c.nombre()));
        }
        return mapa;
    }

    /** Violaciones de FK/CHECK/NOT NULL son un error del caller (400), no del servidor. */
    private static Throwable traducir(Throwable error) {
        if (error instanceof PgException pg && pg.getSqlState() != null
            && SQLSTATE_DATO_INVALIDO.contains(pg.getSqlState())) {
            return new ErrorNegocioException(CodigoError.DATO_RECHAZADO_POR_BASE_DE_DATOS, pg.getErrorMessage());
        }
        return error;
    }
}
