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
import jakarta.ws.rs.QueryParam;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.smallrye.mutiny.Uni;

import com.facturacion.inventario.application.dto.ActualizarItemRequest;
import com.facturacion.inventario.application.dto.CrearItemRequest;
import com.facturacion.inventario.application.dto.ItemResponse;
import com.facturacion.inventario.application.dto.MovimientoKardexResponse;
import com.facturacion.inventario.application.dto.RegistrarAjusteKardexRequest;
import com.facturacion.inventario.application.dto.RegistrarEntradaKardexRequest;
import com.facturacion.inventario.application.service.ItemService;
import com.facturacion.inventario.application.service.KardexService;
import com.facturacion.inventario.domain.model.enums.TipoItem;
import com.facturacion.inventario.infrastructure.security.AccesoEmpresa;
import com.facturacion.commons.security.TokenPrincipal;
import com.facturacion.commons.exception.SinPermisoException;
import com.facturacion.commons.paging.PageRequest;
import com.facturacion.commons.paging.PagedResponse;

/**
 * Catálogo unificado de productos y servicios. Las operaciones sobre
 * existencias (kardex) viven aquí anidadas bajo {@code /items/{id}/kardex}
 * porque siempre pertenecen a un ítem concreto — nunca tiene sentido un
 * movimiento de kardex sin un ítem dueño.
 */
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Ítems", description = "Catálogo de productos y servicios facturables, y su kardex de existencias")
public class ItemResource {

    private final ItemService itemService;
    private final KardexService kardexService;
    private final TokenPrincipal tokenPrincipal;
    private final AccesoEmpresa acceso;

    @Inject
    public ItemResource(ItemService itemService, KardexService kardexService, TokenPrincipal tokenPrincipal,
            AccesoEmpresa acceso) {
        this.itemService = itemService;
        this.kardexService = kardexService;
        this.tokenPrincipal = tokenPrincipal;
        this.acceso = acceso;
    }

    @POST
    @Path("/items")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Crear un ítem (producto o servicio)",
        description = "Si es PRODUCTO y viene con cantidadInicial > 0, se registra de inmediato la primera entrada de kardex.")
    @APIResponse(responseCode = "201", description = "Ítem creado")
    @APIResponse(responseCode = "403", description = "La api-key no es del ruc del ítem")
    public Uni<Response> crear(@Valid CrearItemRequest request, @Context UriInfo uriInfo) {
        tokenPrincipal.exigirEscritura();
        if (!tokenPrincipal.esDeLaEmpresa(request.getRuc())) {
            throw new SinPermisoException(
                "La api-key usada es para el ruc " + tokenPrincipal.getRuc() + ", no para " + request.getRuc());
        }
        return itemService.crear(request)
            .map(response -> {
                URI location = uriInfo.getAbsolutePathBuilder().path(response.getId().toString()).build();
                return Response.created(location).entity(response).build();
            });
    }

    @PUT
    @Path("/items/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Actualizar un ítem", description = "No permite tocar existencias directamente — eso solo cambia vía kardex/reservas.")
    @APIResponse(responseCode = "200", description = "Actualizado")
    @APIResponse(responseCode = "404", description = "Ítem no encontrado")
    public Uni<Response> actualizar(
            @Parameter(description = "Id del ítem", required = true) @PathParam("id") UUID id,
            @Valid ActualizarItemRequest request) {
        return acceso.exigirItemEscritura(id).chain(() -> itemService.actualizar(id, request))
            .map(response -> Response.ok(response).build());
    }

    @GET
    @Path("/items/{id}")
    @Operation(summary = "Obtener un ítem por id")
    @APIResponse(responseCode = "200", description = "Encontrado")
    @APIResponse(responseCode = "404", description = "Ítem no encontrado")
    public Uni<Response> obtenerPorId(
            @Parameter(description = "Id del ítem", required = true) @PathParam("id") UUID id) {
        return acceso.exigirItem(id).chain(() -> itemService.obtenerPorId(id))
            .map(response -> Response.ok(response).build());
    }

    @GET
    @Path("/items")
    @Operation(summary = "Listar ítems", description = "Paginado, con filtro opcional por tipo (PRODUCTO/SERVICIO), categoría y/o ruc de la empresa dueña.")
    @APIResponse(responseCode = "200", description = "Listado")
    public Uni<Response> listar(@Valid @BeanParam PageRequest pageRequest,
            @QueryParam("tipo") TipoItem tipo, @QueryParam("categoriaId") UUID categoriaId,
            @QueryParam("ruc") String ruc) {
        // El listado es siempre de la empresa de la api-key; pedir el de otro RUC es un error, no un resultado vacío.
        if (ruc != null && !tokenPrincipal.esDeLaEmpresa(ruc)) {
            throw new SinPermisoException("La api-key usada es para el ruc " + tokenPrincipal.getRuc()
                + ", no para " + ruc);
        }
        return itemService.listarPaginado(pageRequest.getPage(), pageRequest.getLimit(), tipo, categoriaId,
                tokenPrincipal.getRuc())
            .map((PagedResponse<ItemResponse> paged) -> Response.ok(paged).build());
    }

    @POST
    @Path("/items/{id}/kardex/entradas")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Registrar una entrada de kardex",
        description = "Compra o reposición de stock — recalcula el costo promedio ponderado. Solo válido para ítems PRODUCTO.")
    @APIResponse(responseCode = "201", description = "Movimiento registrado")
    @APIResponse(responseCode = "400", description = "El ítem no es un PRODUCTO")
    @APIResponse(responseCode = "404", description = "Ítem no encontrado")
    public Uni<Response> registrarEntrada(
            @Parameter(description = "Id del ítem", required = true) @PathParam("id") UUID id,
            @Valid RegistrarEntradaKardexRequest request) {
        return acceso.exigirItemEscritura(id).chain(() -> kardexService.registrarEntrada(id, request))
            .map(response -> Response.status(Response.Status.CREATED).entity(response).build());
    }

    @POST
    @Path("/items/{id}/kardex/ajustes")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Registrar un ajuste de kardex",
        description = "Corrección manual por conteo físico o merma — positivo suma al saldo, negativo lo resta.")
    @APIResponse(responseCode = "201", description = "Movimiento registrado")
    @APIResponse(responseCode = "400", description = "El ítem no es un PRODUCTO")
    @APIResponse(responseCode = "404", description = "Ítem no encontrado")
    @APIResponse(responseCode = "409", description = "Stock insuficiente para un ajuste negativo")
    public Uni<Response> registrarAjuste(
            @Parameter(description = "Id del ítem", required = true) @PathParam("id") UUID id,
            @Valid RegistrarAjusteKardexRequest request) {
        return acceso.exigirItemEscritura(id).chain(() -> kardexService.registrarAjuste(id, request))
            .map(response -> Response.status(Response.Status.CREATED).entity(response).build());
    }

    @GET
    @Path("/items/{id}/kardex")
    @Operation(summary = "Listar los movimientos de kardex de un ítem", description = "Paginado, más recientes primero.")
    @APIResponse(responseCode = "200", description = "Listado")
    public Uni<Response> listarKardex(
            @Parameter(description = "Id del ítem", required = true) @PathParam("id") UUID id,
            @Valid @BeanParam PageRequest pageRequest) {
        return acceso.exigirItem(id)
            .chain(() -> kardexService.listarMovimientos(id, pageRequest.getPage(), pageRequest.getLimit()))
            .map((PagedResponse<MovimientoKardexResponse> paged) -> Response.ok(paged).build());
    }
}
