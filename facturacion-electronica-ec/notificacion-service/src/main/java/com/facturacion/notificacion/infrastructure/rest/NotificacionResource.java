package com.facturacion.notificacion.infrastructure.rest;

import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.smallrye.mutiny.Uni;

import com.facturacion.notificacion.application.service.NotificacionService;
import com.facturacion.commons.security.TokenPrincipal;

/** Superficie de observabilidad — el envío real ocurre exclusivamente al consumir ComprobanteAutorizado. */
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Notificaciones", description = "Envío del comprobante autorizado por correo al receptor")
public class NotificacionResource {

    private final NotificacionService notificacionService;
    private final TokenPrincipal tokenPrincipal;

    @Inject
    public NotificacionResource(NotificacionService notificacionService, TokenPrincipal tokenPrincipal) {
        this.notificacionService = notificacionService;
        this.tokenPrincipal = tokenPrincipal;
    }

    @GET
    @Path("/notificaciones/comprobante/{comprobanteId}")
    @Operation(summary = "Estado del envío por correo de un comprobante, por su id en comprobantes-service")
    @APIResponse(responseCode = "200", description = "Encontrado")
    @APIResponse(responseCode = "404", description = "Todavía no se notificó (o nunca se notificará) este comprobante")
    public Uni<Response> obtenerPorComprobanteId(
            @Parameter(description = "Id del comprobante en comprobantes-service", required = true)
            @PathParam("comprobanteId") UUID comprobanteId) {
        return notificacionService.obtenerPorComprobanteId(comprobanteId, tokenPrincipal.getRuc())
            .map(response -> Response.ok(response).build());
    }
}
