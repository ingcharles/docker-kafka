package com.facturacion.commons.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import com.facturacion.commons.exception.TokenInvalidoException;

class JwtVerificadorTest {

    private static final KeyPair EMISOR = par();
    private static final KeyPair AJENO = par();

    private final RevocacionApiKeys revocaciones = new RevocacionApiKeys();
    private final JwtVerificador verificador = new JwtVerificador(base64(EMISOR.getPublic().getEncoded()), revocaciones);
    private final UUID usuario = UUID.randomUUID();

    private static KeyPair par() {
        try {
            return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String base64(byte[] der) {
        return Base64.getEncoder().encodeToString(der);
    }

    /** Api-key válida de identidad-service; cada prueba la altera para romper una sola cosa. */
    private JwtBuilder base(Instant expira) {
        return Jwts.builder().issuer(JwtVerificador.EMISOR).id(UUID.randomUUID().toString())
            .subject(usuario.toString()).claim("tipo", "api_key").claim("ruc", "1722039953001")
            .claim("rol", "ADMIN").claim("establecimiento", "001").expiration(Date.from(expira));
    }

    private String apiKey(Instant expira) {
        return base(expira).signWith(EMISOR.getPrivate()).compact();
    }

    private String sesion() {
        return Jwts.builder().issuer(JwtVerificador.EMISOR).subject(usuario.toString()).claim("tipo", "sesion")
            .expiration(Date.from(Instant.now().plusSeconds(60))).signWith(EMISOR.getPrivate()).compact();
    }

    private void rechaza(String token) {
        TokenInvalidoException e = assertThrows(TokenInvalidoException.class, () -> verificador.verificar(token));
        assertEquals("Token inválido o expirado", e.getMessage());
    }

    @Test
    void apiKeyValidaDevuelveElPrincipalConSuUnicaEmpresaYRol() {
        TokenPrincipal p = verificador.verificar(apiKey(Instant.now().plusSeconds(60)));
        assertEquals(usuario, p.getUsuarioId());
        assertEquals("1722039953001", p.getRuc());
        assertEquals("ADMIN", p.getRol());
        assertEquals("001", p.getEstablecimiento());
        assertNull(p.getPuntoEmision());
    }

    @Test
    void unTokenFirmadoConOtraLlaveSeRechazaAunqueTodoLoDemasSeaValido() {
        rechaza(base(Instant.now().plusSeconds(60)).signWith(AJENO.getPrivate()).compact());
    }

    @Test
    void noSePuedeFalsificarFirmandoConHmacUsandoLaLlavePublicaComoSecreto() {
        // Ataque de confusión de algoritmo: la llave pública es pública, así que sirve de "secreto" HS256 para
        // cualquiera. El verificador debe fijar la llave asimétrica y rechazar el token.
        SecretKey secreto = Keys.hmacShaKeyFor(
            base64(EMISOR.getPublic().getEncoded()).getBytes(StandardCharsets.UTF_8));
        rechaza(base(Instant.now().plusSeconds(60)).signWith(secreto).compact());
    }

    @Test
    void unTokenSinFirmaOConAlgoritmoNoneSeRechaza() {
        rechaza(base(Instant.now().plusSeconds(60)).compact());
    }

    @Test
    void sinElEmisorDeIdentidadSeRechaza() {
        rechaza(base(Instant.now().plusSeconds(60)).issuer("otro-sistema").signWith(EMISOR.getPrivate()).compact());
    }

    @Test
    void unaApiKeyDebeTenerExactamenteUnRucDe13Digitos() {
        Instant vence = Instant.now().plusSeconds(60);
        rechaza(base(vence).claim("ruc", null).signWith(EMISOR.getPrivate()).compact());
        rechaza(base(vence).claim("ruc", "123").signWith(EMISOR.getPrivate()).compact());
        rechaza(base(vence).claim("ruc", "17220399530AB").signWith(EMISOR.getPrivate()).compact());
        // Varias empresas en un mismo token: no es "una única empresa".
        rechaza(base(vence).claim("ruc", List.of("1722039953001", "0190072002001")).signWith(EMISOR.getPrivate())
            .compact());
    }

    @Test
    void unaApiKeyDebeTenerExactamenteUnRolConocido() {
        Instant vence = Instant.now().plusSeconds(60);
        rechaza(base(vence).claim("rol", null).signWith(EMISOR.getPrivate()).compact());
        rechaza(base(vence).claim("rol", "SUPERADMIN").signWith(EMISOR.getPrivate()).compact());
        rechaza(base(vence).claim("rol", List.of("ADMIN", "LECTURA")).signWith(EMISOR.getPrivate()).compact());
        rechaza(base(vence).claim("rol", "admin").signWith(EMISOR.getPrivate()).compact());
    }

    @Test
    void unaApiKeyDebeTenerJtiYVencimiento() {
        Instant vence = Instant.now().plusSeconds(60);
        rechaza(base(vence).id(null).signWith(EMISOR.getPrivate()).compact());
        rechaza(base(vence).expiration(null).signWith(EMISOR.getPrivate()).compact());
    }

    @Test
    void elPuntoDeEmisionExigeEstablecimientoYFormatoValido() {
        Instant vence = Instant.now().plusSeconds(60);
        String conPunto = base(vence).claim("puntoEmision", "002").signWith(EMISOR.getPrivate()).compact();
        assertEquals("002", verificador.verificar(conPunto).getPuntoEmision());

        rechaza(base(vence).claim("establecimiento", null).claim("puntoEmision", "002")
            .signWith(EMISOR.getPrivate()).compact());
        rechaza(base(vence).claim("establecimiento", "1").signWith(EMISOR.getPrivate()).compact());
    }

    @Test
    void unTokenDeSesionNoSirveComoApiKey() {
        assertThrows(TokenInvalidoException.class, () -> verificador.verificar(sesion()));
    }

    @Test
    void soloLosEndpointsPermiteSesionAceptanUnaSesionYSinRuc() {
        TokenPrincipal p = verificador.verificarApiKeyOSesion(sesion());
        assertEquals(usuario, p.getUsuarioId());
        assertNull(p.getRuc());
        assertNull(p.getRol());
    }

    @Test
    void tokenVencidoOManipuladoSeRechazaSinExponerElDetalleDeJjwt() {
        rechaza(apiKey(Instant.now().minusSeconds(60)));

        // Cambia un carácter del medio de la firma (el último puede ser relleno base64 y decodificar igual).
        String valido = apiKey(Instant.now().plusSeconds(60));
        int medio = valido.lastIndexOf('.') + 10;
        char original = valido.charAt(medio);
        rechaza(valido.substring(0, medio) + (original == 'A' ? 'B' : 'A') + valido.substring(medio + 1));
    }

    @Test
    void unaApiKeyRevocadaSeRechazaAunqueSigaVigente() {
        Instant vence = Instant.now().plusSeconds(3600);
        String revocada = base(vence).id("jti-revocado").signWith(EMISOR.getPrivate()).compact();
        String otra = base(vence).id("jti-vigente").signWith(EMISOR.getPrivate()).compact();
        verificador.verificar(revocada); // antes de revocar, pasa

        revocaciones.revocar("jti-revocado", vence);

        TokenInvalidoException e = assertThrows(TokenInvalidoException.class, () -> verificador.verificar(revocada));
        assertEquals("La api-key fue revocada", e.getMessage());
        assertEquals(usuario, verificador.verificar(otra).getUsuarioId());
    }
}
