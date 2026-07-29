package com.facturacion.identidad.infrastructure.crypto;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import com.facturacion.identidad.domain.model.enums.Rol;
import com.facturacion.identidad.shared.exception.TokenInvalidoException;

/**
 * Firma y verifica los JWT de este ecosistema — HS256 con un secreto
 * compartido ({@code IDENTIDAD_JWT_SECRET}), no RSA: no hay necesidad de
 * distribuir una llave pública por separado cuando el único consumidor de
 * cada token es el propio backend (nunca un navegador), y todos los
 * servicios que verifican ya reciben sus secretos por variable de entorno
 * igual que el resto de credenciales del stack.
 * <p>
 * Dos tipos de token, mismo secreto, distinto propósito:
 * <ul>
 *   <li><b>Sesión</b> (login): corta duración, solo sirve para hablar con
 *   este servicio (crear/listar/revocar api-keys) — nunca se manda a
 *   comprobantes-service ni a ningún otro.</li>
 *   <li><b>Api-key</b>: la key ES el JWT (no hay lookup en base de datos
 *   del lado del que la usa) — lleva {@code ruc} y {@code rol} como claims,
 *   que es lo único que comprobantes-service (y el resto) necesitan para
 *   decidir si el caller puede emitir para ese ruc.</li>
 * </ul>
 */
@ApplicationScoped
public class JwtService {

    private static final String CLAIM_TIPO = "tipo";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_RUC = "ruc";
    private static final String CLAIM_ROL = "rol";
    private static final String CLAIM_ESTABLECIMIENTO = "establecimiento";
    private static final String CLAIM_PUNTO_EMISION = "puntoEmision";
    private static final String TIPO_SESION = "sesion";
    private static final String TIPO_API_KEY = "api_key";

    private final SecretKey key;
    private final int sesionDuracionHoras;

    public JwtService(@ConfigProperty(name = "identidad.jwt.secreto") String secreto,
            @ConfigProperty(name = "identidad.jwt.sesion.duracion-horas") int sesionDuracionHoras) {
        this.key = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.sesionDuracionHoras = sesionDuracionHoras;
    }

    public String generarTokenSesion(UUID usuarioId, String email) {
        Instant ahora = Instant.now();
        return Jwts.builder()
            .subject(usuarioId.toString())
            .claim(CLAIM_TIPO, TIPO_SESION)
            .claim(CLAIM_EMAIL, email)
            .issuedAt(Date.from(ahora))
            .expiration(Date.from(ahora.plusSeconds(sesionDuracionHoras * 3600L)))
            .signWith(key)
            .compact();
    }

    /** @return el usuarioId (subject) del token de sesión, ya validado. */
    public UUID validarTokenSesion(String token) {
        Claims claims = parsear(token);
        if (!TIPO_SESION.equals(claims.get(CLAIM_TIPO, String.class))) {
            throw new TokenInvalidoException("El token no es un token de sesión");
        }
        return UUID.fromString(claims.getSubject());
    }

    /**
     * @param establecimiento opcional — acota la key a un establecimiento (ver
     *                        {@code ApiKeyService.crear}); {@code null} = sin restricción.
     * @param puntoEmision    opcional — solo tiene sentido junto con {@code establecimiento}.
     */
    public String generarApiKey(UUID usuarioId, String ruc, Rol rol, String establecimiento, String puntoEmision,
            String jti, Instant expiracion) {
        var builder = Jwts.builder()
            .id(jti)
            .subject(usuarioId.toString())
            .claim(CLAIM_TIPO, TIPO_API_KEY)
            .claim(CLAIM_RUC, ruc)
            .claim(CLAIM_ROL, rol.name())
            .issuedAt(Date.from(Instant.now()))
            .signWith(key);
        if (establecimiento != null) {
            builder.claim(CLAIM_ESTABLECIMIENTO, establecimiento);
        }
        if (puntoEmision != null) {
            builder.claim(CLAIM_PUNTO_EMISION, puntoEmision);
        }
        if (expiracion != null) {
            builder.expiration(Date.from(expiracion));
        }
        return builder.compact();
    }

    private Claims parsear(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new TokenInvalidoException("Token inválido o expirado: " + e.getMessage());
        }
    }
}
