package com.facturacion.identidad.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;

import com.facturacion.commons.limite.LimitadorDeUso;
import com.facturacion.commons.limite.OperacionLimitada;

/**
 * Frena la adivinación de contraseñas: limita los intentos sin éxito de cada cuenta y vuelve a cero cuando uno entra.
 * El intento se cuenta antes de verificar, así una ráfaga en paralelo no se salta el límite. No hay bloqueo permanente:
 * al terminar la ventana la cuenta vuelve a admitir intentos. Se cuenta igual un correo sin cuenta (no delata cuáles
 * existen) y solo se guarda su huella.
 */
@ApplicationScoped
public class LimiteDeLogin {

    private static final String ALGORITMO_HUELLA = "SHA-256";

    private final LimitadorDeUso limitador;

    @Inject
    public LimiteDeLogin(LimitadorDeUso limitador) {
        this.limitador = limitador;
    }

    /** Ejecuta el inicio de sesión si a la cuenta le quedan intentos; si no, 429. */
    public <T> Uni<T> proteger(String email, Supplier<Uni<T>> iniciarSesion) {
        String huella = calcularHuella(UsuarioService.normalizarEmail(email));
        return limitador.exigir(OperacionLimitada.LOGIN, huella, null)
            .chain(() -> iniciarSesion.get())
            .call(() -> limitador.reiniciar(OperacionLimitada.LOGIN, huella, null));
    }

    private static String calcularHuella(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITMO_HUELLA);
            return HexFormat.of().formatHex(digest.digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("La JVM no ofrece " + ALGORITMO_HUELLA, e);
        }
    }
}
