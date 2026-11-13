package com.facturacion.inventario.infrastructure.rest;

import java.net.URI;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
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

import com.facturacion.inventario.application.dto.ActualizarCategoriaRequest;
import com.facturacion.inventario.application.dto.CategoriaResponse;
import com.facturacion.inventario.application.dto.CrearCategoriaRequest;
import com.facturacion.inventario.application.service.CategoriaService;
import com.facturacion.inventario.infrastructure.security.AccesoEmpresa;
import com.facturacion.commons.security.TokenPrincipal;
import com.facturacion.commons.paging.PageRequest;
import com.facturacion.commons.paging.PagedResponse;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Categorías", description = "Clasificación de ítems facturables (producto, servicio, o ambos)")
public class CategoriaResource {

    private final CategoriaService categoriaService;
    private final TokenPrincipal tokenPrincipal;
    private final AccesoEmpresa acceso;

    @Inject
    public CategoriaResource(CategoriaService categoriaService, TokenPrincipal tokenPrincipal,
            AccesoEmpresa acceso) {
        this.categoriaService = categoriaService;
        this.tokenPrincipal = tokenPrincipal;
        this.acceso = acceso;
    }

    @POST
    @Path("/categorias")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Crear una categoría", description = "Queda en la empresa (RUC) de la api-key.")
    @APIResponse(responseCode = "201", description = "Categoría creada")
    public Uni<Response> crear(@Valid CrearCategoriaRequest request, @Context UriInfo uriInfo) {
        tokenPrincipal.exigirEscritura();
        return categoriaService.crear(tokenPrincipal.getRuc(), request)
            .map(response -> {
                URI location = uriInfo.getAbsolutePathBuilder().path(response.getId().toString()).build();
                return Response.created(location).entity(response).build();
            });
    }

    @PUT
    @Path("/categorias/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Actualizar una categoría")
    @APIResponse(responseCode = "200", description = "Actualizada")
    @APIResponse(responseCode = "404", description = "Categoría no encontrada")
    public Uni<Response> actualizar(
            @Parameter(description = "Id de la categoría", required = true) @PathParam("id") UUID id,
            @Valid ActualizarCategoriaRequest request) {
        return acceso.exigirCategoriaEscritura(id).chain(() -> categoriaService.actualizar(id, request))
            .map(response -> Response.ok(response).build());
    }

    @GET
    @Path("/categorias/{id}")
    @Operation(summary = "Obtener una categoría por id")
    @APIResponse(responseCode = "200", description = "Encontrada")
    @APIResponse(responseCode = "404", description = "Categoría no encontrada")
    public Uni<Response> obtenerPorId(
            @Parameter(description = "Id de la categoría", required = true) @PathParam("id") UUID id) {
        return acceso.exigirCategoria(id).chain(() -> categoriaService.obtenerPorId(id))
            .map(response -> Response.ok(response).build());
    }

    @GET
    @Path("/categorias")
    @Operation(summary = "Listar categorías", description = "Paginado; solo las de la empresa de la api-key.")
    @APIResponse(responseCode = "200", description = "Listado")
    public Uni<Response> listar(@Valid @BeanParam PageRequest pageRequest) {
        return categoriaService.listarPaginado(pageRequest.getPage(), pageRequest.getLimit(), tokenPrincipal.getRuc())
            .map((PagedResponse<CategoriaResponse> paged) -> Response.ok(paged).build());
    }
}
