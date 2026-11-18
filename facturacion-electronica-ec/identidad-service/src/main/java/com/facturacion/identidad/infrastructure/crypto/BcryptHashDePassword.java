package com.facturacion.identidad.infrastructure.crypto;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.mutiny.Uni;

import com.facturacion.commons.exception.ErrorNegocioException;
import com.facturacion.commons.reactive.TrabajoBloqueante;
import com.facturacion.identidad.domain.port.HashDePassword;
import com.facturacion.identidad.shared.error.IdentidadError;

/**
 * bcrypt (utilidad estándar de Quarkus: no hay motivo para reinventarlo). Corre en un ejecutor propio y acotado, no en
 * el worker pool compartido: una ráfaga de logins no puede agotar los hilos que usa el resto del servicio. Con todos
 * los hilos ocupados y la cola llena responde 429 ({@link IdentidadError#AUTENTICACION_SATURADA}) en vez de encolar
 * sin límite. Tamaños en {@code identidad.password.hilos} y {@code identidad.password.cola}.
 */
@ApplicationScoped
public class BcryptHashDePassword implements HashDePassword {

    private static final String PREFIJO_HILO = "bcrypt-";
    private static final String PASSWORD_DESCARTABLE = "contraseña-descartable-sin-cuenta";

    /** Hash de una contraseña que nadie conoce: sirve para gastar el mismo tiempo cuando no hay cuenta. */
    private final String hashDescartable = BcryptUtil.bcryptHash(PASSWORD_DESCARTABLE);
    private final ThreadPoolExecutor ejecutor;

    @Inject
    public BcryptHashDePassword(@ConfigProperty(name = "identidad.password.hilos") int hilos,
            @ConfigProperty(name = "identidad.password.cola") int cola) {
        AtomicInteger numero = new AtomicInteger();
        this.ejecutor = new ThreadPoolExecutor(hilos, hilos, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(cola),
            tarea -> {
                Thread hilo = new Thread(tarea, PREFIJO_HILO + numero.incrementAndGet());
                hilo.setDaemon(true);
                return hilo;
            });
    }

    @Override
    public Uni<String> generar(String password) {
        return traducirSaturacion(TrabajoBloqueante.ejecutar(() -> BcryptUtil.bcryptHash(password), ejecutor));
    }

    @Override
    public Uni<Boolean> verificar(String password, String hash) {
        boolean hayCuenta = hash != null;
        String hashAComparar = hayCuenta ? hash : hashDescartable;
        return traducirSaturacion(TrabajoBloqueante.ejecutar(
            () -> BcryptUtil.matches(password, hashAComparar) && hayCuenta, ejecutor));
    }

    private static <T> Uni<T> traducirSaturacion(Uni<T> uni) {
        return uni.onFailure(RejectedExecutionException.class)
            .transform(falla -> new ErrorNegocioException(IdentidadError.AUTENTICACION_SATURADA));
    }

    @PreDestroy
    void cerrar() {
        ejecutor.shutdownNow();
    }
}
