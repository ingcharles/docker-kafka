package com.facturacion.orquestacion.domain.port;

import java.util.UUID;

import io.smallrye.mutiny.Uni;

/** Puerto de salida del patrón inbox: recuerda qué mensajes ya se procesaron para no repetirlos (idempotencia). */
public interface MensajesProcesados {

    Uni<Boolean> esMensajeProcesado(UUID mensajeId);

    Uni<Void> marcarProcesado(UUID mensajeId);
}
