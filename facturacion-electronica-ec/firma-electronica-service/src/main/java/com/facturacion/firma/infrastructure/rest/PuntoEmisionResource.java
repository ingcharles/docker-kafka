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

import com.facturacion.firma.application.dto.ActualizarPuntoEmisionRequest;
import com.facturacion.firma.application.dto.CrearPuntoEmisionRequest;
import com.facturacion.firma.infrastructure.security.AccesoEmpresa;
import com.facturacion.firma.application.dto.PuntoEmisionResponse;
import com.facturacion.firma.application.service.PuntoEmisionService;

/** CRUD de puntos de emisión de un establecimiento — ver {@link PuntoEmisionService}. */
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Puntos de emisión", description = "Puntos de emisión (ptoEmi) de un establecimiento")
public class PuntoEmisionResource {

    private final PuntoEmisionService puntoEmisionService;
    private final AccesoEmpresa acceso;

    @Inject
    public PuntoEmisionResource(PuntoEmisionService puntoEmisionService, AccesoEmpresa acceso) {
        this.puntoEmisionService = puntoEmisionService;
        this.acceso = acceso;
    }

    @POST
    @Path("/{empresaId}/establecimientos/{establecimientoId}/puntos-emision")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Registrar un punto de emisión", description = "Código de 3 dígitos (ptoEmi de la ficha técnica del SRI), único por establecimiento.")
    @APIResponse(responseCode = "201", description = "Punto de emisión creado")
    @APIResponse(responseCode = "404", description = "Empresa o establecimiento no encontrado")
    @APIResponse(responseCode = "409", description = "Ya existe un punto de emisión con ese código para este establecimiento")
    public Uni<Response> crear(
            @Parameter(description = "Id de la empresa", required = true) @PathParam("empresaId") UUID empresaId,
            @Parameter(description = "Id del establecimiento", required = true)
            @PathParam("establecimientoId") UUID establecimientoId,
            @Valid CrearPuntoEmisionRequest request, @Context UriInfo uriInfo) {
        return acceso.exigirEstablecimientoAdmin(empresaId, establecimientoId)
            .chain(() -> puntoEmisionService.crear(empresaId, establecimientoId, request))
            .map(response -> {
                URI location = uriInfo.getBaseUriBuilder().path("puntos-emision")
                    .path(response.getId().toString()).build();
                return Response.created(location).entity(response).build();
            });
    }

    @PUT
    @Path("/{empresaId}/establecimientos/{establecimientoId}/puntos-emision/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Actualizar un punto de emisión")
    @APIResponse(responseCode = "200", description = "Actualizado")
    @APIResponse(responseCode = "404", description = "Empresa, establecimiento o punto de emisión no encontrado")
    public Uni<Response> actualizar(
            @Parameter(description = "Id de la empresa", required = true) @PathParam("empresaId") UUID empresaId,
            @Parameter(description = "Id del establecimiento", required = true)
            @PathParam("establecimientoId") UUID establecimientoId,
            @Parameter(description = "Id del punto de emisión", required = true) @PathParam("id") UUID id,
            @Valid ActualizarPuntoEmisionRequest request) {
        return acceso.exigirEstablecimientoAdmin(empresaId, establecimientoId)
            .chain(() -> puntoEmisionService.actualizar(empresaId, establecimientoId, id, request))
            .map(response -> Response.ok(response).build());
    }

    @GET
    @Path("/{empresaId}/establecimientos/{establecimientoId}/puntos-emision")
    @Operation(summary = "Listar los puntos de emisión de un establecimiento")
    @APIResponse(responseCode = "200", description = "Listado")
    public Uni<Response> listar(
            @Parameter(description = "Id de la empresa", required = true) @PathParam("empresaId") UUID empresaId,
            @Parameter(description = "Id del establecimiento", required = true)
            @PathParam("establecimientoId") UUID establecimientoId) {
        return acceso.exigirEstablecimiento(empresaId, establecimientoId)
            .chain(() -> puntoEmisionService.listarPorEstablecimiento(establecimientoId))
            .map((List<PuntoEmisionResponse> puntos) -> Response.ok(puntos).build());
    }

    @GET
    @Path("/{empresaId}/establecimientos/{establecimientoId}/puntos-emision/{id}")
    @Operation(summary = "Obtener un punto de emisión por id")
    @APIResponse(responseCode = "200", description = "Encontrado")
    @APIResponse(responseCode = "404", description = "Empresa, establecimiento o punto de emisión no encontrado")
    public Uni<Response> obtener(
            @Parameter(description = "Id de la empresa", required = true) @PathParam("empresaId") UUID empresaId,
            @Parameter(description = "Id del establecimiento", required = true)
            @PathParam("establecimientoId") UUID establecimientoId,
            @Parameter(description = "Id del punto de emisión", required = true) @PathParam("id") UUID id) {
        return acceso.exigirEstablecimiento(empresaId, establecimientoId)
            .chain(() -> puntoEmisionService.obtener(establecimientoId, id))
            .map(response -> Response.ok(response).build());
    }
}
