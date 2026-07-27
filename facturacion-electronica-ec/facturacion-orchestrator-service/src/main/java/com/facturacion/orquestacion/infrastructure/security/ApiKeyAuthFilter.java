package com.facturacion.orquestacion.infrastructure.security;

import java.io.IOException;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import com.facturacion.orquestacion.shared.exception.TokenInvalidoException;

/**
 * Filtro global (sin {@code @NameBinding} a propósito: todo endpoint de
 * este servicio requiere una api-key válida, no hay rutas públicas) — exige
 * {@code Authorization: Bearer <api-key>} emitida por identidad-service y
 * la verifica localmente vía {@link JwtVerificador}, sin llamada de red.
 * No intercepta los endpoints de gestión de Quarkus ({@code /q/health},
 * {@code /q/swagger-ui}, etc.) porque esos no pasan por el filtro JAX-RS.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class ApiKeyAuthFilter implements ContainerRequestFilter {

    private final JwtVerificador jwtVerificador;
    private final TokenPrincipal tokenPrincipal;

    @Inject
    public ApiKeyAuthFilter(JwtVerificador jwtVerificador, TokenPrincipal tokenPrincipal) {
        this.jwtVerificador = jwtVerificador;
        this.tokenPrincipal = tokenPrincipal;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String header = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity("Falta el header Authorization: Bearer <api-key>").build());
            return;
        }
        try {
            TokenPrincipal verificado = jwtVerificador.verificar(header.substring("Bearer ".length()));
            tokenPrincipal.setUsuarioId(verificado.getUsuarioId());
            tokenPrincipal.setRuc(verificado.getRuc());
            tokenPrincipal.setRol(verificado.getRol());
        } catch (TokenInvalidoException e) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build());
        }
    }
}
