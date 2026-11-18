package com.facturacion.commons.error;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;

/**
 * Lee el estado SQL (SQLSTATE) de la causa de una excepción de base de datos, sin depender del cliente concreto: el
 * cliente reactivo de Postgres ({@code PgException.getSqlState()}), JDBC ({@link SQLException#getSQLState()}) o
 * cualquier excepción que tenga uno de esos dos métodos, aunque Hibernate la haya envuelto varias veces.
 * <p>
 * Sirve para distinguir "la base de datos está ocupada" (se venció un plazo y conviene reintentar: 503) de un error
 * real (500).
 */
public final class EstadoSql {

    /** {@code lock_timeout}: esperar un bloqueo de fila o de tabla tardó más de lo permitido. */
    public static final String BLOQUEO_NO_DISPONIBLE = "55P03";
    /** {@code statement_timeout}: la sentencia tardó más de lo permitido (la base la canceló). */
    public static final String CONSULTA_CANCELADA = "57014";
    /** {@code idle_in_transaction_session_timeout}: una transacción quedó abierta e inactiva y la base la cerró. */
    public static final String TRANSACCION_INACTIVA_VENCIDA = "25P03";

    private static final Set<String> PLAZOS_VENCIDOS =
        Set.of(BLOQUEO_NO_DISPONIBLE, CONSULTA_CANCELADA, TRANSACCION_INACTIVA_VENCIDA);
    private static final String[] METODOS_DE_ESTADO = {"getSqlState", "getSQLState"};
    private static final int PROFUNDIDAD_MAXIMA = 16;

    private EstadoSql() {
    }

    /** El primer estado SQL que aparezca en la excepción o en sus causas. */
    public static Optional<String> buscarEn(Throwable falla) {
        Throwable actual = falla;
        for (int i = 0; actual != null && i < PROFUNDIDAD_MAXIMA; i++) {
            Optional<String> estado = leerDe(actual);
            if (estado.isPresent()) {
                return estado;
            }
            actual = actual.getCause() == actual ? null : actual.getCause();
        }
        return Optional.empty();
    }

    /** Si el estado indica que un plazo de la base de datos venció (bloqueo, sentencia o transacción inactiva). */
    public static boolean esPlazoVencido(String estadoSql) {
        return estadoSql != null && PLAZOS_VENCIDOS.contains(estadoSql);
    }

    private static Optional<String> leerDe(Throwable falla) {
        if (falla instanceof SQLException sql && sql.getSQLState() != null) {
            return Optional.of(sql.getSQLState());
        }
        for (String nombre : METODOS_DE_ESTADO) {
            try {
                Method metodo = falla.getClass().getMethod(nombre);
                if (metodo.getReturnType() == String.class) {
                    Object valor = metodo.invoke(falla);
                    if (valor instanceof String texto && !texto.isBlank()) {
                        return Optional.of(texto);
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                // esa excepción no tiene el método (o no se pudo llamar): se prueba con la siguiente causa
                continue;
            }
        }
        return Optional.empty();
    }
}
