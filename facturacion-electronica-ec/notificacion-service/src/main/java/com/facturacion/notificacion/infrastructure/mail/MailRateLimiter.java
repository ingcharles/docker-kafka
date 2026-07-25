package com.facturacion.notificacion.infrastructure.mail;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.smallrye.mutiny.Uni;

/**
 * Espacia los envíos de correo con al menos {@code intervaloMinimoMs} entre
 * uno y el siguiente, sin importar cuántos lleguen en paralelo (el tópico
 * {@code facturacion.comprobante.eventos.v1} tiene varias particiones, así
 * que varios comprobantes autorizados pueden llegar casi al mismo tiempo).
 * Mandar muchos correos de golpe desde la misma cuenta es justo el patrón
 * que los proveedores SMTP (Gmail incluido) usan para marcar una cuenta
 * como origen de spam o aplicarle throttling/bloqueo temporal — este
 * limitador es la forma barata de evitarlo sin depender de un proveedor de
 * envío masivo dedicado.
 *
 * Implementado sin locks (CAS vía {@link AtomicLong#accumulateAndGet}) y
 * sin bloquear ningún hilo: el que le toca esperar recibe un {@link Uni}
 * que se resuelve más tarde vía el temporizador de Vert.x
 * ({@code onItem().delayIt()}), no un {@code Thread.sleep}.
 */
@ApplicationScoped
public class MailRateLimiter {

    private final long intervaloMinimoMs;
    private final AtomicLong proximoTurnoLibre = new AtomicLong(0);

    public MailRateLimiter(
            @ConfigProperty(name = "notificacion.smtp.intervalo-minimo-ms", defaultValue = "2000") long intervaloMinimoMs) {
        this.intervaloMinimoMs = intervaloMinimoMs;
    }

    public Uni<Void> esperarTurno() {
        long ahora = System.currentTimeMillis();
        long proximoTrasReservar = proximoTurnoLibre.accumulateAndGet(ahora,
            (turnoReservado, ahoraCapturado) -> Math.max(turnoReservado, ahoraCapturado) + intervaloMinimoMs);
        long miTurno = proximoTrasReservar - intervaloMinimoMs;
        long espera = miTurno - ahora;

        if (espera <= 0) {
            return Uni.createFrom().voidItem();
        }
        return Uni.createFrom().voidItem().onItem().delayIt().by(Duration.ofMillis(espera));
    }
}
