package com.facturacion.identidad.infrastructure.rest;

import java.io.IOException;
import java.util.UUID;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import com.facturacion.identidad.infrastructure.crypto.JwtService;
import com.facturacion.commons.exception.TokenInvalidoException;

/**
 * Exige {@code Authorization: Bearer <token de sesión>} en cualquier
 * endpoint anotado con {@link Autenticado} — el token se obtiene de
 * {@code POST /usuarios/login}. Deja el {@code usuarioId} ya validado en
 * {@link SesionActual} para que el recurso no tenga que volver a tocar JWT.
 */
@Provider
@Autenticado
@Priority(Priorities.AUTHENTICATION)
public class SesionAuthFilter implements ContainerRequestFilter {

    private final JwtService jwtService;
    private final SesionActual sesionActual;

    @Inject
    public SesionAuthFilter(JwtService jwtService, SesionActual sesionActual) {
        this.jwtService = jwtService;
        this.sesionActual = sesionActual;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String header = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity("Falta el header Authorization: Bearer <token>").build());
            return;
        }
        try {
            UUID usuarioId = jwtService.validarTokenSesion(header.substring("Bearer ".length()));
            sesionActual.setUsuarioId(usuarioId);
        } catch (TokenInvalidoException e) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity(e.getMessage()).build());
        }
    }
}
