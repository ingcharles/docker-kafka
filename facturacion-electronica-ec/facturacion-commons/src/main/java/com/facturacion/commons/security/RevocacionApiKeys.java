package com.facturacion.commons.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Lista local de api-keys revocadas (por {@code jti}) que cada servicio mantiene en memoria. identidad-service publica
 * cada revocación en {@code facturacion.apikey.revocaciones.v1} y {@link RevocacionApiKeysConsumer} la carga aquí; así
 * {@link JwtVerificador} sigue verificando sin llamada de red y aun así rechaza una key revocada antes de que venza.
 * <p>
 * Cada entrada guarda el vencimiento del token: pasado ese instante el token ya no es válido por sí mismo, así que la
 * entrada se descarta y la lista no crece sin límite.
 */
@ApplicationScoped
public class RevocacionApiKeys {

    private final Map<String, Instant> revocadas = new ConcurrentHashMap<>();

    public void revocar(String jti, Instant venceEn) {
        Instant ahora = Instant.now();
        if (venceEn.isAfter(ahora)) {
            revocadas.put(jti, venceEn);
        }
        revocadas.values().removeIf(vence -> !vence.isAfter(ahora));
    }

    public boolean estaRevocada(String jti) {
        if (jti == null) {
            return false;
        }
        Instant vence = revocadas.get(jti);
        return vence != null && vence.isAfter(Instant.now());
    }

    int contarRevocadas() {
        return revocadas.size();
    }
}
