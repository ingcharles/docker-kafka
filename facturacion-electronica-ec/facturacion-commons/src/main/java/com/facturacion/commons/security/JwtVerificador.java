package com.facturacion.commons.security;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.crypto.SecretKey;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import com.facturacion.commons.exception.TokenInvalidoException;

/**
 * Verifica localmente (sin llamada de red) el JWT que identidad-service emitió como api-key — mismo secreto HS256
 * ({@code IDENTIDAD_JWT_SECRET}) en ambos lados. Solo verifica, nunca firma: ningún servicio salvo identidad emite
 * tokens. Esta única implementación reemplaza las copias que había en cada microservicio.
 * <p>
 * El mensaje que recibe el cliente es genérico: el detalle de JJWT (firma, expiración, formato) solo va al log, porque
 * le serviría a un atacante para distinguir un token mal firmado de uno vencido.
 */
@ApplicationScoped
public class JwtVerificador {

    private static final Logger LOG = Logger.getLogger(JwtVerificador.class);

    private static final String CLAIM_TIPO = "tipo";
    private static final String CLAIM_RUC = "ruc";
    private static final String CLAIM_ROL = "rol";
    private static final String CLAIM_ESTABLECIMIENTO = "establecimiento";
    private static final String CLAIM_PUNTO_EMISION = "puntoEmision";
    private static final String TIPO_API_KEY = "api_key";
    private static final String TIPO_SESION = "sesion";

    private final SecretKey key;

    public JwtVerificador(@ConfigProperty(name = "identidad.jwt.secreto") String secreto) {
        this.key = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }

    public TokenPrincipal verificar(String token) {
        Claims claims = parsear(token);
        if (!TIPO_API_KEY.equals(claims.get(CLAIM_TIPO, String.class))) {
            throw new TokenInvalidoException("El token no es una api-key");
        }
        return principalDeApiKey(claims);
    }

    /**
     * Solo para endpoints {@link PermiteSesion}: acepta también un token de sesión de identidad-service, con
     * {@code ruc}/{@code rol}/{@code establecimiento}/{@code puntoEmision} en {@code null}.
     */
    public TokenPrincipal verificarApiKeyOSesion(String token) {
        Claims claims = parsear(token);
        String tipo = claims.get(CLAIM_TIPO, String.class);
        if (TIPO_API_KEY.equals(tipo)) {
            return principalDeApiKey(claims);
        }
        if (TIPO_SESION.equals(tipo)) {
            TokenPrincipal principal = new TokenPrincipal();
            principal.setUsuarioId(UUID.fromString(claims.getSubject()));
            return principal;
        }
        throw new TokenInvalidoException("El token no es una api-key ni un token de sesión válido");
    }

    private TokenPrincipal principalDeApiKey(Claims claims) {
        TokenPrincipal principal = new TokenPrincipal();
        principal.setUsuarioId(UUID.fromString(claims.getSubject()));
        principal.setRuc(claims.get(CLAIM_RUC, String.class));
        principal.setRol(claims.get(CLAIM_ROL, String.class));
        // null en tokens emitidos antes de que estos claims existieran — sin restricción, comportamiento sin cambios.
        principal.setEstablecimiento(claims.get(CLAIM_ESTABLECIMIENTO, String.class));
        principal.setPuntoEmision(claims.get(CLAIM_PUNTO_EMISION, String.class));
        return principal;
    }

    private Claims parsear(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            LOG.debugf("Token rechazado: %s", e.getMessage());
            throw new TokenInvalidoException("Token inválido o expirado", e);
        }
    }
}
