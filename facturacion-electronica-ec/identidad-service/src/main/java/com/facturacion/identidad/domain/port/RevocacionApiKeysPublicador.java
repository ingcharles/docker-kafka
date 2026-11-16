package com.facturacion.identidad.domain.port;

import java.time.Instant;

import io.smallrye.mutiny.Uni;

/**
 * Puerto de salida: avisa al resto de servicios que una api-key dejó de valer. El {@code Uni} termina cuando el
 * mensaje fue aceptado por el broker, de modo que quien revoca no responde éxito si el aviso no salió.
 */
public interface RevocacionApiKeysPublicador {

    Uni<Void> publicar(String jti, Instant venceEn);
}
