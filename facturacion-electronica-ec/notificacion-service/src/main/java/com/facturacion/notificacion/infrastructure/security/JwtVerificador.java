package com.facturacion.notificacion.infrastructure.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import com.facturacion.notificacion.shared.exception.TokenInvalidoException;

/**
 * Verifica localmente (sin llamada de red) el JWT que identidad-service
 * emitió como api-key — mismo secreto HS256 ({@code IDENTIDAD_JWT_SECRET})
 * en ambos lados. Copia independiente de la clase homónima de
 * identidad-service (sin librerías compartidas entre microservicios, mismo
 * criterio que el resto del stack) — solo verifica, nunca firma: este
 * servicio no emite tokens.
 */
@ApplicationScoped
public class JwtVerificador {

    private static final String CLAIM_TIPO = "tipo";
    private static final String CLAIM_RUC = "ruc";
    private static final String CLAIM_ROL = "rol";
    private static final String CLAIM_ESTABLECIMIENTO = "establecimiento";
    private static final String CLAIM_PUNTO_EMISION = "puntoEmision";
    private static final String TIPO_API_KEY = "api_key";

    private final SecretKey key;

    public JwtVerificador(@ConfigProperty(name = "identidad.jwt.secreto") String secreto) {
        this.key = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }

    public TokenPrincipal verificar(String token) {
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new TokenInvalidoException("Token inválido o expirado: " + e.getMessage());
        }
        if (!TIPO_API_KEY.equals(claims.get(CLAIM_TIPO, String.class))) {
            throw new TokenInvalidoException("El token no es una api-key");
        }
        TokenPrincipal principal = new TokenPrincipal();
        principal.setUsuarioId(java.util.UUID.fromString(claims.getSubject()));
        principal.setRuc(claims.get(CLAIM_RUC, String.class));
        principal.setRol(claims.get(CLAIM_ROL, String.class));
        // null en tokens emitidos antes de que estos claims existieran — sin restricción, comportamiento sin cambios.
        principal.setEstablecimiento(claims.get(CLAIM_ESTABLECIMIENTO, String.class));
        principal.setPuntoEmision(claims.get(CLAIM_PUNTO_EMISION, String.class));
        return principal;
    }
}
