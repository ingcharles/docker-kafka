package com.facturacion.commons.limite;

import java.time.Duration;

import io.smallrye.mutiny.Uni;

/** Contador compartido por todas las réplicas de un servicio. */
public interface ContadorDeUso {

    /** Suma un uso a la clave y devuelve lo acumulado en su ventana, en una sola operación atómica. */
    Uni<Uso> consumir(String clave, Duration ventana);

    /** Olvida lo consumido por la clave. */
    Uni<Void> reiniciar(String clave);
}
