package com.facturacion.identidad.domain.port;

import io.smallrye.mutiny.Uni;

/**
 * Hash y verificación de contraseñas. El algoritmo es costoso a propósito (decenas de milisegundos de CPU), así que
 * las dos operaciones devuelven un {@code Uni}: quien las implementa las corre fuera del event loop y vuelve al
 * contexto del llamador. El caso de uso no sabe de hilos ni de algoritmo.
 */
public interface HashDePassword {

    /** El hash de {@code password}, listo para guardar. */
    Uni<String> generar(String password);

    /**
     * Si {@code password} corresponde a {@code hash}. Con {@code hash} nulo (no hay cuenta) responde {@code false}
     * pero gasta el mismo tiempo que una verificación real: si no, la respuesta tardaría menos y delataría qué
     * correos tienen cuenta.
     */
    Uni<Boolean> verificar(String password, String hash);
}
