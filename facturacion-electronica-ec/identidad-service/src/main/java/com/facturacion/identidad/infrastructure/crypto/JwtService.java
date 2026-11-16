package com.facturacion.identidad.infrastructure.crypto;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import com.facturacion.commons.exception.TokenInvalidoException;
import com.facturacion.commons.security.ClavesJwt;
import com.facturacion.commons.security.JwtVerificador;
import com.facturacion.identidad.domain.model.enums.Rol;
import com.facturacion.identidad.domain.port.EmisorTokens;

/**
 * Firma los JWT de este ecosistema con una llave privada Ed25519 (o RSA) que solo existe en este servicio
 * ({@code IDENTIDAD_JWT_CLAVE_PRIVADA}); los demás servicios verifican con la pública
 * ({@code IDENTIDAD_JWT_CLAVE_PUBLICA}, ver {@code JwtVerificador} en facturacion-commons). Antes todos compartían un
 * secreto HS256: comprometer cualquier servicio permitía fabricar tokens de cualquier empresa y rol. Ahora un servicio
 * comprometido solo tiene la llave pública y no puede emitir nada.
 * <p>
 * Dos tipos de token, misma llave, distinto propósito:
 * <ul>
 *   <li><b>Sesión</b> (login): corta duración, solo sirve para hablar con este servicio (crear/listar/revocar
 *   api-keys) y para dar de alta una empresa — nunca para operar una empresa.</li>
 *   <li><b>Api-key</b>: la key ES el JWT (no hay lookup en base de datos del lado del que la usa). Vale para
 *   <b>una única empresa y un único rol</b>: lleva exactamente un {@code ruc} y un {@code rol} como claims, que es lo
 *   único que el resto de servicios necesita para decidir si el caller puede operar esa empresa. El rol sale de la
 *   asociación usuario↔empresa al emitirla, nunca de lo que pida el cliente.</li>
 * </ul>
 */
@ApplicationScoped
public class JwtService implements EmisorTokens {

    private static final String CLAIM_TIPO = "tipo";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_RUC = "ruc";
    private static final String CLAIM_ROL = "rol";
    private static final String CLAIM_ESTABLECIMIENTO = "establecimiento";
    private static final String CLAIM_PUNTO_EMISION = "puntoEmision";
    private static final String TIPO_SESION = "sesion";
    private static final String TIPO_API_KEY = "api_key";

    private final PrivateKey clavePrivada;
    private final PublicKey clavePublica;
    private final int sesionDuracionHoras;

    public JwtService(@ConfigProperty(name = "identidad.jwt.clave-privada") String clavePrivada,
            @ConfigProperty(name = "identidad.jwt.clave-publica") String clavePublica,
            @ConfigProperty(name = "identidad.jwt.sesion.duracion-horas") int sesionDuracionHoras) {
        this.clavePrivada = ClavesJwt.privada(clavePrivada);
        this.clavePublica = ClavesJwt.publica(clavePublica);
        this.sesionDuracionHoras = sesionDuracionHoras;
    }

    @Override
    public String generarTokenSesion(UUID usuarioId, String email) {
        Instant ahora = Instant.now();
        return Jwts.builder()
            .issuer(JwtVerificador.EMISOR)
            .subject(usuarioId.toString())
            .claim(CLAIM_TIPO, TIPO_SESION)
            .claim(CLAIM_EMAIL, email)
            .issuedAt(Date.from(ahora))
            .expiration(Date.from(ahora.plusSeconds(sesionDuracionHoras * 3600L)))
            .signWith(clavePrivada)
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
     * @param expiracion      obligatoria: una api-key sin vencimiento no se acepta al verificarla.
     */
    @Override
    public String generarApiKey(UUID usuarioId, String ruc, Rol rol, String establecimiento, String puntoEmision,
            String jti, Instant expiracion) {
        if (expiracion == null) {
            throw new IllegalArgumentException("Una api-key debe tener fecha de vencimiento");
        }
        var builder = Jwts.builder()
            .issuer(JwtVerificador.EMISOR)
            .id(jti)
            .subject(usuarioId.toString())
            .claim(CLAIM_TIPO, TIPO_API_KEY)
            .claim(CLAIM_RUC, ruc)
            .claim(CLAIM_ROL, rol.name())
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(expiracion))
            .signWith(clavePrivada);
        if (establecimiento != null) {
            builder.claim(CLAIM_ESTABLECIMIENTO, establecimiento);
        }
        if (puntoEmision != null) {
            builder.claim(CLAIM_PUNTO_EMISION, puntoEmision);
        }
        return builder.compact();
    }

    private Claims parsear(String token) {
        try {
            return Jwts.parser().verifyWith(clavePublica).requireIssuer(JwtVerificador.EMISOR).build()
                .parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            // El detalle de JJWT (firma, vencimiento, formato) no se devuelve al cliente.
            throw new TokenInvalidoException("Token inválido o expirado", e);
        }
    }
}
