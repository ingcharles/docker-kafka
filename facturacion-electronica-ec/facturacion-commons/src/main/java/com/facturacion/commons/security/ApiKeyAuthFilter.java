package com.facturacion.commons.security;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;

import com.facturacion.commons.exception.TokenInvalidoException;

/**
 * Filtro global (sin {@code @NameBinding} a propósito: todo endpoint requiere una api-key válida, no hay rutas
 * públicas) — exige {@code Authorization: Bearer <api-key>} emitida por identidad-service y la verifica localmente
 * vía {@link JwtVerificador}, sin llamada de red. No intercepta los endpoints de gestión de Quarkus
 * ({@code /q/health}, {@code /q/swagger-ui}, etc.) porque esos no pasan por el filtro JAX-RS.
 * <p>
 * Un endpoint marcado con {@link PermiteSesion} acepta también un token de sesión. Un token ausente o inválido se
 * reporta lanzando {@link TokenInvalidoException}: el {@code ApiExceptionMapper} la convierte en el 401 con su código
 * de catálogo (antes era texto plano armado a mano en cada servicio).
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class ApiKeyAuthFilter implements ContainerRequestFilter {

    private static final String PREFIJO = "Bearer ";

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
    public void filter(ContainerRequestContext requestContext) {
        boolean permiteSesion = permiteSesion();
        String header = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(PREFIJO)) {
            throw new TokenInvalidoException(permiteSesion
                ? "Falta el header Authorization: Bearer <api-key o token de sesión>"
                : "Falta el header Authorization: Bearer <api-key>");
        }
        String token = header.substring(PREFIJO.length());
        TokenPrincipal verificado = permiteSesion ? jwtVerificador.verificarApiKeyOSesion(token)
            : jwtVerificador.verificar(token);
        tokenPrincipal.setUsuarioId(verificado.getUsuarioId());
        tokenPrincipal.setRuc(verificado.getRuc());
        tokenPrincipal.setRol(verificado.getRol());
        tokenPrincipal.setEstablecimiento(verificado.getEstablecimiento());
        tokenPrincipal.setPuntoEmision(verificado.getPuntoEmision());
    }

    private boolean permiteSesion() {
        return resourceInfo != null && resourceInfo.getResourceMethod() != null
            && resourceInfo.getResourceMethod().isAnnotationPresent(PermiteSesion.class);
    }
}
