package com.ecommerce.shipping.infrastructure.rest;

import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.smallrye.mutiny.Uni;

import com.ecommerce.shipping.application.service.ShipmentService;
import com.ecommerce.shipping.domain.model.enums.ShipmentStatus;
import com.ecommerce.shipping.shared.requests.PageRequest;

/** Solo lectura — los envíos únicamente se crean o actualizan mediante los comandos de Kafka CreateShipment/CancelShipment. */
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Shipments", description = "Shipment records, driven entirely by saga commands")
public class ShipmentResource {

    private final ShipmentService shipmentService;

    @Inject
    public ShipmentResource(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @GET
    @Path("/{orderId}")
    @Operation(summary = "Get the shipment for an order")
    @APIResponse(responseCode = "200", description = "Found")
    @APIResponse(responseCode = "404", description = "No shipment recorded for this order")
    public Uni<Response> getByOrderId(
        @Parameter(description = "Order id", required = true) @PathParam("orderId") UUID orderId) {
        return shipmentService.getByOrderId(orderId).map(response -> Response.ok(response).build());
    }

    @GET
    @Operation(summary = "List shipments", description = "Paged, optionally filtered by status.")
    @APIResponse(responseCode = "200", description = "Listing")
    public Uni<Response> list(@Valid @BeanParam PageRequest pageRequest,
        @QueryParam("status") ShipmentStatus status) {
        return shipmentService.listPaged(pageRequest.getPage(), pageRequest.getLimit(), status)
            .map(paged -> Response.ok(paged).build());
    }
}
