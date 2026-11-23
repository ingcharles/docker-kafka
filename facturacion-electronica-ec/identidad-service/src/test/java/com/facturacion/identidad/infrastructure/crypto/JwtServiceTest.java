package com.facturacion.identidad.infrastructure.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.facturacion.commons.exception.TokenInvalidoException;
import com.facturacion.commons.security.JwtVerificador;
import com.facturacion.commons.security.RevocacionApiKeys;
import com.facturacion.commons.security.TokenPrincipal;
import com.facturacion.identidad.domain.model.enums.Rol;

class JwtServiceTest {

    private static final KeyPair LLAVES = par();

    private final JwtService jwt = servicio(LLAVES, 8);

    private static KeyPair par() {
        try {
            return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static JwtService servicio(KeyPair llaves, int horas) {
        return new JwtService(Base64.getEncoder().encodeToString(llaves.getPrivate().getEncoded()),
            Base64.getEncoder().encodeToString(llaves.getPublic().getEncoded()), horas);
    }

    @Test
    void testDeberiaValidarUnTokenDeSesionYDevolverElUsuario() {
        UUID usuario = UUID.randomUUID();
        assertEquals(usuario, jwt.validarTokenSesion(jwt.generarTokenSesion(usuario, "a@x.com")));
    }

    @Test
    void testDeberiaRechazarUnaApiKeyUsadaComoTokenDeSesion() {
        String apiKey = jwt.generarApiKey(UUID.randomUUID(), "1722039953001", Rol.ADMIN, null, null, "jti-1",
            Instant.now().plusSeconds(60));
        assertThrows(TokenInvalidoException.class, () -> jwt.validarTokenSesion(apiKey));
    }

    @Test
    void testDeberiaRechazarUnTokenManipulado() {
        String token = jwt.generarTokenSesion(UUID.randomUUID(), "a@x.com");
        int medio = token.lastIndexOf('.') + 10;
        char original = token.charAt(medio);
        String manipulado = token.substring(0, medio) + (original == 'A' ? 'B' : 'A') + token.substring(medio + 1);
        assertThrows(TokenInvalidoException.class, () -> jwt.validarTokenSesion(manipulado));
    }

    @Test
    void testDeberiaRechazarUnTokenFirmadoConOtraLlave() {
        String token = servicio(par(), 8).generarTokenSesion(UUID.randomUUID(), "a@x.com");
        TokenInvalidoException e = assertThrows(TokenInvalidoException.class, () -> jwt.validarTokenSesion(token));
        assertEquals("Token inválido o expirado", e.getMessage());
    }

    @Test
    void testDeberiaRechazarUnTokenExpirado() {
        String token = servicio(LLAVES, -1).generarTokenSesion(UUID.randomUUID(), "a@x.com");
        assertThrows(TokenInvalidoException.class, () -> jwt.validarTokenSesion(token));
    }

    @Test
    void testDeberiaRechazarUnaCadenaQueNoEsJwt() {
        assertThrows(TokenInvalidoException.class, () -> jwt.validarTokenSesion("esto-no-es-un-jwt"));
    }

    @Test
    void testDeberiaVerificarConSoloLaLlavePublicaLaApiKeyQueEmiteIdentidad() {
        UUID usuario = UUID.randomUUID();
        String apiKey = jwt.generarApiKey(usuario, "1722039953001", Rol.OPERADOR, "001", "002", "jti-9",
            Instant.now().plusSeconds(3600));
        // Los demás servicios solo tienen la llave pública.
        JwtVerificador otroServicio = new JwtVerificador(
            Base64.getEncoder().encodeToString(LLAVES.getPublic().getEncoded()), new RevocacionApiKeys());

        TokenPrincipal principal = otroServicio.verificar(apiKey);

        assertEquals(usuario, principal.getUsuarioId());
        assertEquals("1722039953001", principal.getRuc());
        assertEquals("OPERADOR", principal.getRol());
        assertEquals("001", principal.getEstablecimiento());
        assertEquals("002", principal.getPuntoEmision());
    }

    @Test
    void testDeberiaRechazarEnTodosLosServiciosUnaApiKeyEmitidaPorOtraLlave() {
        String falsa = servicio(par(), 8).generarApiKey(UUID.randomUUID(), "1722039953001", Rol.ADMIN, null, null,
            "jti-x", Instant.now().plusSeconds(3600));
        JwtVerificador otroServicio = new JwtVerificador(
            Base64.getEncoder().encodeToString(LLAVES.getPublic().getEncoded()), new RevocacionApiKeys());

        assertThrows(TokenInvalidoException.class, () -> otroServicio.verificar(falsa));
    }

    @Test
    void testDeberiaNoEmitirUnaApiKeySinVencimiento() {
        assertThrows(IllegalArgumentException.class,
            () -> jwt.generarApiKey(UUID.randomUUID(), "1722039953001", Rol.ADMIN, null, null, "jti", null));
    }

    @Test
    void testDeberiaEmitirCadaApiKeyConUnSoloRucYUnSoloRolTomadosDeLaAsociacion() {
        String apiKey = jwt.generarApiKey(UUID.randomUUID(), "1722039953001", Rol.LECTURA, null, null, "jti-2",
            Instant.now().plusSeconds(60));
        String payload = new String(Base64.getUrlDecoder().decode(apiKey.split("\\.")[1]));

        assertTrue(payload.contains("\"ruc\":\"1722039953001\""));
        assertTrue(payload.contains("\"rol\":\"LECTURA\""));
    }
}
