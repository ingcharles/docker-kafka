package com.facturacion.firma.infrastructure.rest;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.smallrye.mutiny.Uni;

import com.facturacion.commons.rest.Respuestas;
import com.facturacion.firma.application.dto.ActualizarEstablecimientoRequest;
import com.facturacion.firma.application.dto.CrearEstablecimientoRequest;
import com.facturacion.firma.application.dto.EstablecimientoResponse;
import com.facturacion.firma.application.service.EstablecimientoService;
import com.facturacion.firma.infrastructure.security.AccesoEmpresa;

/**
 * CRUD de establecimientos de una empresa — ver {@link EstablecimientoService}.
 * Basado en id (no en ruc), igual que {@code PUT actualizar}/certificado en
 * {@link EmpresaResource}, así que no re-chequea la titularidad de la
 * api-key aquí.
 */
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Establecimientos", description = "Establecimientos (estab) de una empresa")
public class EstablecimientoResource {

    private final EstablecimientoService establecimientoService;
    private final AccesoEmpresa acceso;

    @Inject
    public EstablecimientoResource(EstablecimientoService establecimientoService, AccesoEmpresa acceso) {
        this.establecimientoService = establecimientoService;
        this.acceso = acceso;
    }

    @POST
    @Path("/{empresaId}/establecimientos")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Registrar un establecimiento", description = "Código de 3 dígitos (estab de la ficha técnica del SRI), único por empresa.")
    @APIResponse(responseCode = "201", description = "Establecimiento creado")
    @APIResponse(responseCode = "404", description = "Empresa no encontrada")
    @APIResponse(responseCode = "409", description = "Ya existe un establecimiento con ese código para esta empresa")
    public Uni<Response> crear(
            @Parameter(description = "Id de la empresa", required = true) @PathParam("empresaId") UUID empresaId,
            @Valid CrearEstablecimientoRequest request, @Context UriInfo uriInfo) {
        return acceso.exigirEmpresaAdmin(empresaId).chain(() -> establecimientoService.crear(empresaId, request))
            .map(response -> {
                URI location = uriInfo.getBaseUriBuilder().path("establecimientos")
                    .path(response.getId().toString()).build();
                return Respuestas.creado(location, response);
            });
    }

    @PUT
    @Path("/{empresaId}/establecimientos/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Actualizar un establecimiento")
    @APIResponse(responseCode = "200", description = "Actualizado")
    @APIResponse(responseCode = "404", description = "Empresa o establecimiento no encontrado")
    public Uni<Response> actualizar(
            @Parameter(description = "Id de la empresa", required = true) @PathParam("empresaId") UUID empresaId,
            @Parameter(description = "Id del establecimiento", required = true) @PathParam("id") UUID id,
            @Valid ActualizarEstablecimientoRequest request) {
        return acceso.exigirEmpresaAdmin(empresaId).chain(() -> establecimientoService.actualizar(empresaId, id, request))
            .map(response -> Respuestas.actualizado(response));
    }

    @GET
    @Path("/{empresaId}/establecimientos")
    @Operation(summary = "Listar los establecimientos de una empresa")
    @APIResponse(responseCode = "200", description = "Listado")
    public Uni<Response> listar(
            @Parameter(description = "Id de la empresa", required = true) @PathParam("empresaId") UUID empresaId) {
        return acceso.exigirEmpresa(empresaId).chain(() -> establecimientoService.listarPorEmpresa(empresaId))
            .map((List<EstablecimientoResponse> establecimientos) -> Response.ok(establecimientos).build());
    }

    @GET
    @Path("/{empresaId}/establecimientos/{id}")
    @Operation(summary = "Obtener un establecimiento por id")
    @APIResponse(responseCode = "200", description = "Encontrado")
    @APIResponse(responseCode = "404", description = "Empresa o establecimiento no encontrado")
    public Uni<Response> obtener(
            @Parameter(description = "Id de la empresa", required = true) @PathParam("empresaId") UUID empresaId,
            @Parameter(description = "Id del establecimiento", required = true) @PathParam("id") UUID id) {
        return acceso.exigirEmpresa(empresaId).chain(() -> establecimientoService.obtener(empresaId, id))
            .map(response -> Response.ok(response).build());
    }
}
