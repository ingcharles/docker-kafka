package com.facturacion.commons.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.security.KeyPairGenerator;
import java.util.Base64;

import jakarta.ws.rs.container.ContainerRequestContext;

import org.junit.jupiter.api.Test;

import com.facturacion.commons.exception.TokenInvalidoException;

/** El filtro global exige api-key; un servicio con endpoints públicos (identidad) lo apaga por configuración. */
class ApiKeyAuthFilterTest {

    private static JwtVerificador verificador() throws Exception {
        String publica = Base64.getEncoder()
            .encodeToString(KeyPairGenerator.getInstance("Ed25519").generateKeyPair().getPublic().getEncoded());
        return new JwtVerificador(publica, new RevocacionApiKeys());
    }

    /** Una petición sin cabecera Authorization. */
    private static ContainerRequestContext sinCabecera() {
        return (ContainerRequestContext) Proxy.newProxyInstance(ApiKeyAuthFilterTest.class.getClassLoader(),
            new Class<?>[] {ContainerRequestContext.class}, (proxy, metodo, args) -> {
                if (metodo.getName().equals("getHeaderString")) {
                    return null;
                }
                throw new UnsupportedOperationException(metodo.getName());
            });
    }

    @Test
    void habilitadoExigeLaApiKey() throws Exception {
        ApiKeyAuthFilter filtro = new ApiKeyAuthFilter(verificador(), new TokenPrincipal(), true);

        assertThrows(TokenInvalidoException.class, () -> filtro.filter(sinCabecera()));
    }

    @Test
    void apagadoDejaPasarLasPeticionesSinApiKeyComoElRegistroYElLogin() throws Exception {
        ApiKeyAuthFilter filtro = new ApiKeyAuthFilter(verificador(), new TokenPrincipal(), false);

        assertDoesNotThrow(() -> filtro.filter(sinCabecera()));
    }
}
