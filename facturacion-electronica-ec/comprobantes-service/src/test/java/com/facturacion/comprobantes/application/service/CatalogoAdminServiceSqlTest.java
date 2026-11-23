package com.facturacion.comprobantes.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.facturacion.comprobantes.domain.catalogo.CatalogoDef;
import com.facturacion.comprobantes.domain.model.enums.Catalogo;

/**
 * El SQL del upsert se genera a partir de la definición del catálogo; estos tests fijan su forma. Un bug real que
 * ya ocurrió: declarar el parámetro como {@code $n::numeric} hace que el cliente de Postgres exija un tipo numérico y
 * rechace el String con el que viajan los DECIMAL y las fechas — y ningún test unitario miraba el SQL.
 */
class CatalogoAdminServiceSqlTest {

    private static String sql(Catalogo catalogo) {
        return CatalogoAdminService.armarUpsertSql(CatalogoDef.buscar(catalogo.nombre()).orElseThrow());
    }

    @Test
    void testDeberiaDeclararDecimalesYFechasComoTextoAntesDeConvertirlos() {
        String sql = sql(Catalogo.TARIFA_IMPUESTO);
        assertTrue(sql.contains("$4::text::numeric"), sql); // tarifa
        assertTrue(sql.contains("$6::text::date"), sql);    // vigente_desde
        assertTrue(sql.contains("$7::text::date"), sql);    // vigente_hasta
    }

    @Test
    void testDeberiaNoDeclararNingunParametroDirectamenteComoNumericoNiFecha() {
        for (Catalogo catalogo : Catalogo.values()) {
            String sql = sql(catalogo);
            assertFalse(sql.matches("(?s).*\\$\\d+::(numeric|date).*"), catalogo + ": " + sql);
        }
    }

    @Test
    void testDeberiaUsarLaClaveCompletaDelCatalogoEnElUpsert() {
        String sql = sql(Catalogo.RETENCION);
        assertTrue(sql.contains("on conflict (impuesto_codigo, codigo, vigente_desde) do update set"), sql);
        assertTrue(sql.startsWith("insert into retencion ("), sql);
        assertTrue(sql.endsWith("activo"), sql);
    }

    @Test
    void testDeberiaNoActualizarUnaColumnaClaveEnElDoUpdate() {
        String sql = sql(Catalogo.FORMA_PAGO);
        String actualizacion = sql.substring(sql.indexOf("do update set"), sql.indexOf("returning"));
        assertFalse(actualizacion.contains("codigo = EXCLUDED.codigo"), actualizacion);
        assertTrue(actualizacion.contains("descripcion = EXCLUDED.descripcion"), actualizacion);
        assertEquals(1, sql.split("returning", -1).length - 1);
    }
}
