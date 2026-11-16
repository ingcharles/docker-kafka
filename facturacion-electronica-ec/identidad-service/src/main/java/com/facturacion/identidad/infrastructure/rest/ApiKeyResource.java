package com.facturacion.identidad.infrastructure.rest;

import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.smallrye.mutiny.Uni;

import com.facturacion.commons.rest.Respuestas;
import com.facturacion.identidad.application.dto.ApiKeyResponse;
import com.facturacion.identidad.application.dto.CrearApiKeyRequest;
import com.facturacion.identidad.application.service.ApiKeyService;

@Path("/api-keys")
@Produces(MediaType.APPLICATION_JSON)
@Autenticado
@SecurityRequirement(name = "token")
@Tag(name = "Api keys", description = "Credenciales que el resto del ecosistema usa para llamar a comprobantes-service y afines — ver ARQUITECTURA.md.")
public class ApiKeyResource {

    private final ApiKeyService apiKeyService;
    private final SesionActual sesionActual;

    @Inject
    public ApiKeyResource(ApiKeyService apiKeyService, SesionActual sesionActual) {
        this.apiKeyService = apiKeyService;
        this.sesionActual = sesionActual;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Crear una api-key", description = "El valor solo se muestra en esta respuesta — no se puede volver a consultar después.")
    @APIResponse(responseCode = "201", description = "Api-key creada, con su valor")
    @APIResponse(responseCode = "403", description = "El usuario no está asociado al ruc indicado")
    @APIResponse(responseCode = "409", description = "Ya existe una api-key con ese nombre para este usuario")
    public Uni<Response> crear(@Valid CrearApiKeyRequest request) {
        return apiKeyService.crear(sesionActual.getUsuarioId(), request)
            .map(response -> Respuestas.creado(response));
    }

    @GET
    @Operation(summary = "Listar las api-keys del usuario autenticado", description = "Nunca incluye el valor real de la key.")
    public Uni<List<ApiKeyResponse>> listar() {
        return apiKeyService.listar(sesionActual.getUsuarioId());
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Revocar una api-key", description = "Efectiva al instante en todos los servicios (se publica en facturacion.apikey.revocaciones.v1). Es idempotente.")
    @APIResponse(responseCode = "204", description = "Revocada")
    @APIResponse(responseCode = "404", description = "No existe, o no es del usuario autenticado")
    public Uni<Response> revocar(@Parameter(description = "Id de la api-key") @PathParam("id") UUID id) {
        return apiKeyService.revocar(sesionActual.getUsuarioId(), id)
            .map(ignorado -> Respuestas.sinContenido());
    }
}
