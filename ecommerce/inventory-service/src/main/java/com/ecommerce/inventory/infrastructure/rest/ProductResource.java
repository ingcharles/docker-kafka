package com.ecommerce.inventory.infrastructure.rest;

import java.net.URI;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
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

import com.ecommerce.inventory.application.dto.CreateProductRequest;
import com.ecommerce.inventory.application.dto.ProductResponse;
import com.ecommerce.inventory.application.dto.StockReservationResponse;
import com.ecommerce.inventory.application.service.ProductService;
import com.ecommerce.inventory.application.service.StockReservationService;
import com.ecommerce.inventory.shared.requests.PageRequest;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Products", description = "Product catalog and stock levels — reservation itself happens only via Kafka commands")
public class ProductResource {

    private final ProductService productService;
    private final StockReservationService stockReservationService;

    @Inject
    public ProductResource(ProductService productService, StockReservationService stockReservationService) {
        this.productService = productService;
        this.stockReservationService = stockReservationService;
    }

    @POST
    @Path("/products")
    @Operation(summary = "Create a product", description = "Seeds the catalog with a SKU and its initial stock.")
    @APIResponse(responseCode = "201", description = "Product created")
    @APIResponse(responseCode = "400", description = "Invalid request")
    public Uni<Response> create(@Valid CreateProductRequest request, @Context UriInfo uriInfo) {
        return productService.createProduct(request)
            .map(response -> {
                URI location = uriInfo.getAbsolutePathBuilder().path(response.getId().toString()).build();
                return Response.created(location).entity(response).build();
            });
    }

    @GET
    @Path("/products/{id}")
    @Operation(summary = "Get a product by id, including available/reserved quantity")
    @APIResponse(responseCode = "200", description = "Found")
    @APIResponse(responseCode = "404", description = "Product not found")
    public Uni<Response> getById(
        @Parameter(description = "Product id", required = true) @PathParam("id") UUID id) {
        return productService.getById(id).map(response -> Response.ok(response).build());
    }

    @GET
    @Path("/products")
    @Operation(summary = "List products", description = "Paged catalog listing.")
    @APIResponse(responseCode = "200", description = "Listing")
    public Uni<Response> list(@Valid @BeanParam PageRequest pageRequest) {
        return productService.listPaged(pageRequest.getPage(), pageRequest.getLimit())
            .map(paged -> Response.ok(paged).build());
    }

    @GET
    @Path("/products/{id}/reservations")
    @Operation(summary = "List stock reservations for a product",
        description = "Observability endpoint: every RESERVED/RELEASED row touching this product.")
    @APIResponse(responseCode = "200", description = "Listing")
    public Uni<Response> listReservations(
        @Parameter(description = "Product id", required = true) @PathParam("id") UUID id) {
        return stockReservationService.listReservationsForProduct(id)
            .map((java.util.List<StockReservationResponse> list) -> Response.ok(list).build());
    }
}
