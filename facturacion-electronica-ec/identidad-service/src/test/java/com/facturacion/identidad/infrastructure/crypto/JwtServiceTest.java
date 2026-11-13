package com.facturacion.identidad.infrastructure.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.facturacion.identidad.domain.model.enums.Rol;
import com.facturacion.commons.exception.TokenInvalidoException;

class JwtServiceTest {

    private static final String SECRETO = "secreto-de-prueba-con-al-menos-32-bytes-ok!!";

    private final JwtService jwt = new JwtService(SECRETO, 8);

    @Test
    void unTokenDeSesionSeValidaYDevuelveElUsuario() {
        UUID usuario = UUID.randomUUID();
        assertEquals(usuario, jwt.validarTokenSesion(jwt.generarTokenSesion(usuario, "a@x.com")));
    }

    @Test
    void unaApiKeyNoSirveComoTokenDeSesion() {
        String apiKey = jwt.generarApiKey(UUID.randomUUID(), "1722039953001", Rol.ADMIN, null, null, "jti-1",
            Instant.now().plusSeconds(60));
        assertThrows(TokenInvalidoException.class, () -> jwt.validarTokenSesion(apiKey));
    }

    @Test
    void unTokenManipuladoSeRechaza() {
        String token = jwt.generarTokenSesion(UUID.randomUUID(), "a@x.com");
        String manipulado = token.substring(0, token.length() - 3) + (token.endsWith("AAA") ? "BBB" : "AAA");
        assertThrows(TokenInvalidoException.class, () -> jwt.validarTokenSesion(manipulado));
    }

    @Test
    void unTokenFirmadoConOtroSecretoSeRechaza() {
        JwtService ajeno = new JwtService("otro-secreto-distinto-con-32-bytes-o-mas!!", 8);
        String token = ajeno.generarTokenSesion(UUID.randomUUID(), "a@x.com");
        assertThrows(TokenInvalidoException.class, () -> jwt.validarTokenSesion(token));
    }

    @Test
    void unTokenExpiradoSeRechaza() {
        JwtService caducado = new JwtService(SECRETO, -1);
        String token = caducado.generarTokenSesion(UUID.randomUUID(), "a@x.com");
        assertThrows(TokenInvalidoException.class, () -> jwt.validarTokenSesion(token));
    }

    @Test
    void unaCadenaQueNoEsJwtSeRechaza() {
        assertThrows(TokenInvalidoException.class, () -> jwt.validarTokenSesion("esto-no-es-un-jwt"));
    }
}
