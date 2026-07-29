package com.facturacion.firma.infrastructure.security;

import java.io.IOException;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import com.facturacion.firma.shared.exception.TokenInvalidoException;

/**
 * Filtro global (sin {@code @NameBinding} a propósito: todo endpoint de
 * este servicio requiere una api-key válida, no hay rutas públicas) — exige
 * {@code Authorization: Bearer <api-key>} emitida por identidad-service y
 * la verifica localmente vía {@link JwtVerificador}, sin llamada de red.
 * No intercepta los endpoints de gestión de Quarkus ({@code /q/health},
 * {@code /q/swagger-ui}, etc.) porque esos no pasan por el filtro JAX-RS.
 * <p>
 * Única excepción: el endpoint marcado con {@link PermiteSesion}
 * ({@code EmpresaResource.crear}) también acepta un token de sesión — ver
 * su javadoc para el porqué.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class ApiKeyAuthFilter implements ContainerRequestFilter {

    private final JwtVerificador jwtVerificador;
    private final TokenPrincipal tokenPrincipal;

    @Context
    ResourceInfo resourceInfo;

    @Inject
    public ApiKeyAuthFilter(JwtVerificador jwtVerificador, TokenPrincipal tokenPrincipal) {
        this.jwtVerificador = jwtVerificador;
        this.tokenPrincipal = tokenPrincipal;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String header = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        String mensajeFalta = permiteSesion()
            ? "Falta el header Authorization: Bearer <api-key o token de sesión>"
            : "Falta el header Authorization: Bearer <api-key>";
        if (header == null || !header.startsWith("Bearer ")) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity(mensajeFalta).build());
            return;
        }
        try {
            String token = header.substring("Bearer ".length());
            TokenPrincipal verificado = permiteSesion() ? jwtVerificador.verificarApiKeyOSesion(token)
                : jwtVerificador.verificar(token);
            tokenPrincipal.setUsuarioId(verificado.getUsuarioId());
            tokenPrincipal.setRuc(verificado.getRuc());
            tokenPrincipal.setRol(verificado.getRol());
            tokenPrincipal.setEstablecimiento(verificado.getEstablecimiento());
            tokenPrincipal.setPuntoEmision(verificado.getPuntoEmision());
        } catch (TokenInvalidoException e) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build());
        }
    }

    private boolean permiteSesion() {
        return resourceInfo.getResourceMethod() != null
            && resourceInfo.getResourceMethod().isAnnotationPresent(PermiteSesion.class);
    }
}
