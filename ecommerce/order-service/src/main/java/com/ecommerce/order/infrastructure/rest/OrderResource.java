package com.ecommerce.order.infrastructure.rest;

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

import com.ecommerce.order.application.dto.CreateOrderRequest;
import com.ecommerce.order.application.dto.OrderResponse;
import com.ecommerce.order.application.service.OrderService;
import com.ecommerce.order.domain.model.enums.OrderStatus;
import com.ecommerce.order.shared.requests.PageRequest;
import com.ecommerce.order.shared.responses.PagedResponse;

/**
 * Una vez creada, la orden solo se modifica a través de los comandos de la
 * saga — a propósito, aquí no existe ningún endpoint PATCH ni PUT. El dueño
 * del estado de la orden es el orquestador de la saga, que lo cambia vía el
 * tópico {@code ecommerce.order.commands.v1}.
 */
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Orders", description = "Order aggregate — entry point of the e-commerce saga")
public class OrderResource {

    private final OrderService orderService;

    @Inject
    public OrderResource(OrderService orderService) {
        this.orderService = orderService;
    }

    @POST
    @Operation(summary = "Create an order", description = "Persists the order and stages its "
        + "OrderCreated event in the outbox, kicking off the saga.")
    @APIResponse(responseCode = "201", description = "Order created")
    @APIResponse(responseCode = "400", description = "Invalid request")
    public Uni<Response> create(@Valid CreateOrderRequest request, @Context UriInfo uriInfo) {
        return orderService.createOrder(request)
            .map(response -> {
                URI location = uriInfo.getAbsolutePathBuilder().path(response.getId().toString()).build();
                return Response.created(location).entity(response).build();
            });
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get an order by id")
    @APIResponse(responseCode = "200", description = "Found")
    @APIResponse(responseCode = "404", description = "Order not found")
    public Uni<Response> getById(
        @Parameter(description = "Order id", required = true) @PathParam("id") UUID id) {
        return orderService.getById(id).map(response -> Response.ok(response).build());
    }

    @GET
    @Operation(summary = "List orders", description = "Paged, optionally filtered by customerId and/or status.")
    @APIResponse(responseCode = "200", description = "Listing")
    public Uni<Response> list(@Valid @BeanParam PageRequest pageRequest,
        @QueryParam("customerId") String customerId,
        @QueryParam("status") OrderStatus status) {
        return orderService.listPaged(pageRequest.getPage(), pageRequest.getLimit(), customerId, status)
            .map((PagedResponse<OrderResponse> paged) -> Response.ok(paged).build());
    }
}
