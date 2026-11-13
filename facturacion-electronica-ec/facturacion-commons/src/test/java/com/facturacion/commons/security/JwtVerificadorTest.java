package com.facturacion.commons.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import com.facturacion.commons.exception.TokenInvalidoException;

class JwtVerificadorTest {

    private static final String SECRETO = "un-secreto-de-prueba-con-mas-de-treinta-y-dos-bytes";
    private static final SecretKey CLAVE = Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8));

    private final JwtVerificador verificador = new JwtVerificador(SECRETO);
    private final UUID usuario = UUID.randomUUID();

    private String apiKey(Instant expira) {
        return Jwts.builder().subject(usuario.toString()).claim("tipo", "api_key").claim("ruc", "1722039953001")
            .claim("rol", "ADMIN").claim("establecimiento", "001").expiration(Date.from(expira)).signWith(CLAVE)
            .compact();
    }

    private String sesion() {
        return Jwts.builder().subject(usuario.toString()).claim("tipo", "sesion")
            .expiration(Date.from(Instant.now().plusSeconds(60))).signWith(CLAVE).compact();
    }

    @Test
    void apiKeyValidaDevuelveElPrincipalConSusClaims() {
        TokenPrincipal p = verificador.verificar(apiKey(Instant.now().plusSeconds(60)));
        assertEquals(usuario, p.getUsuarioId());
        assertEquals("1722039953001", p.getRuc());
        assertEquals("ADMIN", p.getRol());
        assertEquals("001", p.getEstablecimiento());
        assertNull(p.getPuntoEmision());
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
        TokenInvalidoException vencido = assertThrows(TokenInvalidoException.class,
            () -> verificador.verificar(apiKey(Instant.now().minusSeconds(60))));
        assertEquals("Token inválido o expirado", vencido.getMessage());

        // Cambia un carácter del medio de la firma (el último puede ser relleno base64 y decodificar igual).
        String valido = apiKey(Instant.now().plusSeconds(60));
        int medio = valido.lastIndexOf('.') + 10;
        char original = valido.charAt(medio);
        String manipulado = valido.substring(0, medio) + (original == 'A' ? 'B' : 'A') + valido.substring(medio + 1);
        TokenInvalidoException firmaMala = assertThrows(TokenInvalidoException.class,
            () -> verificador.verificar(manipulado));
        assertEquals("Token inválido o expirado", firmaMala.getMessage());
    }
}
