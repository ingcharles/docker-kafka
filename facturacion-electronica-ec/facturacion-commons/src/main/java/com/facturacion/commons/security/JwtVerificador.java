package com.facturacion.commons.security;

import java.security.PublicKey;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import com.facturacion.commons.exception.TokenInvalidoException;

/**
 * Verifica localmente (sin llamada de red) el JWT que identidad-service emitió como api-key o sesión. La firma es
 * asimétrica (Ed25519 o RSA): este servicio solo tiene la llave pública
 * ({@code identidad.jwt.clave-publica}), así que comprometerlo NO permite fabricar tokens; solo identidad-service,
 * que guarda la privada, puede emitirlos. Solo verifica, nunca firma.
 * <p>
 * Una api-key vale para <b>una única empresa y un único rol</b>: debe traer exactamente un {@code ruc} (13 dígitos) y
 * un {@code rol} (ADMIN, OPERADOR o LECTURA) como texto, un {@code jti} (para poder revocarla) y un vencimiento
 * ({@code exp}); si falta o no tiene esa forma se rechaza, aunque la firma sea válida. Además se exige el emisor
 * ({@code iss}) de identidad-service.
 * <p>
 * El mensaje que recibe el cliente es genérico: el detalle de JJWT (firma, expiración, formato) solo va al log, porque
 * le serviría a un atacante para distinguir un token mal firmado de uno vencido.
 */
@ApplicationScoped
public class JwtVerificador {

    /** Valor del claim {@code iss} con el que identidad-service firma todos sus tokens. */
    public static final String EMISOR = "facturacion-identidad";

    private static final Logger LOG = Logger.getLogger(JwtVerificador.class);

    private static final String CLAIM_TIPO = "tipo";
    private static final String CLAIM_RUC = "ruc";
    private static final String CLAIM_ROL = "rol";
    private static final String CLAIM_ESTABLECIMIENTO = "establecimiento";
    private static final String CLAIM_PUNTO_EMISION = "puntoEmision";
    private static final String TIPO_API_KEY = "api_key";
    private static final String TIPO_SESION = "sesion";
    private static final String MENSAJE_GENERICO = "Token inválido o expirado";
    private static final Pattern RUC = Pattern.compile("\\d{13}");
    private static final Pattern CODIGO_TRES_DIGITOS = Pattern.compile("\\d{3}");
    private static final Set<String> ROLES = Set.of("ADMIN", "OPERADOR", "LECTURA");

    private final PublicKey clavePublica;
    private final RevocacionApiKeys revocaciones;

    public JwtVerificador(@ConfigProperty(name = "identidad.jwt.clave-publica") String clavePublica,
            RevocacionApiKeys revocaciones) {
        this.clavePublica = ClavesJwt.publica(clavePublica);
        this.revocaciones = revocaciones;
    }

    public TokenPrincipal verificar(String token) {
        Claims claims = parsear(token);
        if (!TIPO_API_KEY.equals(tipoDe(claims))) {
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
        String tipo = tipoDe(claims);
        if (TIPO_API_KEY.equals(tipo)) {
            return principalDeApiKey(claims);
        }
        if (TIPO_SESION.equals(tipo)) {
            TokenPrincipal principal = new TokenPrincipal();
            principal.setUsuarioId(usuarioDe(claims));
            return principal;
        }
        throw new TokenInvalidoException("El token no es una api-key ni un token de sesión válido");
    }

    private TokenPrincipal principalDeApiKey(Claims claims) {
        String ruc = texto(claims, CLAIM_RUC);
        String rol = texto(claims, CLAIM_ROL);
        String establecimiento = texto(claims, CLAIM_ESTABLECIMIENTO);
        String puntoEmision = texto(claims, CLAIM_PUNTO_EMISION);
        if (ruc == null || !RUC.matcher(ruc).matches()) {
            throw rechazo("la api-key no trae exactamente un ruc de 13 dígitos");
        }
        if (rol == null || !ROLES.contains(rol)) {
            throw rechazo("la api-key no trae exactamente un rol válido");
        }
        if (claims.getId() == null || claims.getId().isBlank()) {
            throw rechazo("la api-key no trae jti (no se podría revocar)");
        }
        if (claims.getExpiration() == null) {
            throw rechazo("la api-key no vence");
        }
        if (establecimiento != null && !CODIGO_TRES_DIGITOS.matcher(establecimiento).matches()) {
            throw rechazo("establecimiento con formato inválido");
        }
        if (puntoEmision != null && (establecimiento == null || !CODIGO_TRES_DIGITOS.matcher(puntoEmision).matches())) {
            throw rechazo("puntoEmision sin establecimiento o con formato inválido");
        }
        TokenPrincipal principal = new TokenPrincipal();
        principal.setUsuarioId(usuarioDe(claims));
        principal.setRuc(ruc);
        principal.setRol(rol);
        // null en tokens sin acotar — sin restricción.
        principal.setEstablecimiento(establecimiento);
        principal.setPuntoEmision(puntoEmision);
        return principal;
    }

    private Claims parsear(String token) {
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(clavePublica).requireIssuer(EMISOR).build().parseSignedClaims(token)
                .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            LOG.debugf("Token rechazado: %s", e.getMessage());
            throw new TokenInvalidoException(MENSAJE_GENERICO, e);
        }
        // La firma, el emisor y el vencimiento ya son válidos; falta saber si identidad-service la revocó antes de que venza.
        if (revocaciones.estaRevocada(claims.getId())) {
            throw new TokenInvalidoException("La api-key fue revocada");
        }
        return claims;
    }

    private String tipoDe(Claims claims) {
        return texto(claims, CLAIM_TIPO);
    }

    private UUID usuarioDe(Claims claims) {
        try {
            return UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw rechazo("el subject no es un usuario válido");
        }
    }

    /** El claim como texto; {@code null} si no existe. Si existe pero no es texto (p. ej. una lista) se rechaza. */
    private String texto(Claims claims, String nombre) {
        Object valor = claims.get(nombre);
        if (valor == null) {
            return null;
        }
        if (!(valor instanceof String texto)) {
            throw rechazo("el claim " + nombre + " debe ser un único texto");
        }
        return texto;
    }

    private TokenInvalidoException rechazo(String motivo) {
        LOG.debugf("Token rechazado: %s", motivo);
        return new TokenInvalidoException(MENSAJE_GENERICO);
    }
}
