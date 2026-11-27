package com.facturacion.commons.limite;

import java.util.Optional;

import io.smallrye.mutiny.Uni;

/** Qué límite le toca a una empresa en una operación. */
public interface ResolutorDeLimites {

    /**
     * El límite propio de la empresa y, si no tiene, el general. Vacío si no hay ninguno configurado.
     *
     * @param ruc empresa por la que se hace la operación; {@code null} cuando no hay ninguna (p. ej. el login)
     */
    Uni<Optional<Limite>> resolver(OperacionLimitada operacion, String ruc);
}
