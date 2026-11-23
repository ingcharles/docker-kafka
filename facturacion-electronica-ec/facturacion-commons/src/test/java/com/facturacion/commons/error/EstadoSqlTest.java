package com.facturacion.commons.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/** El 503 se decide por el estado SQL de la causa, aunque venga envuelta; un error real de SQL sigue siendo un error. */
class EstadoSqlTest {

    /** Imita al PgException del cliente reactivo de Postgres: un {@code getSqlState()} y nada más. */
    private static final class FalloDePostgres extends RuntimeException {
        private final String estado;

        FalloDePostgres(String estado) {
            super("ERROR: canceling statement due to lock timeout");
            this.estado = estado;
        }

        public String getSqlState() {
            return estado;
        }
    }

    @Test
    void testDeberiaLeerElEstadoSqlDelClienteReactivoAunqueEsteEnvueltoVariasVeces() {
        Throwable envuelta = new RuntimeException("persistence",
            new IllegalStateException("hibernate", new FalloDePostgres(EstadoSql.BLOQUEO_NO_DISPONIBLE)));

        assertEquals(Optional.of("55P03"), EstadoSql.buscarEn(envuelta));
    }

    @Test
    void testDeberiaLeerElEstadoSqlDeJdbc() {
        assertEquals(Optional.of("57014"), EstadoSql.buscarEn(new SQLException("cancelada", "57014")));
    }

    @Test
    void testDeberiaNoDevolverNadaSiLaFallaNoTraeEstadoSql() {
        assertEquals(Optional.empty(), EstadoSql.buscarEn(new RuntimeException("otra cosa", new IllegalStateException())));
        assertEquals(Optional.empty(), EstadoSql.buscarEn(null));
    }

    @Test
    void testDeberiaConsiderarRetriablesSoloLosPlazosVencidos() {
        assertTrue(EstadoSql.esPlazoVencido(EstadoSql.BLOQUEO_NO_DISPONIBLE));
        assertTrue(EstadoSql.esPlazoVencido(EstadoSql.CONSULTA_CANCELADA));
        assertTrue(EstadoSql.esPlazoVencido(EstadoSql.TRANSACCION_INACTIVA_VENCIDA));
        assertFalse(EstadoSql.esPlazoVencido("23505"), "una violación de unicidad es un error real, no un plazo");
        assertFalse(EstadoSql.esPlazoVencido(null));
    }
}
