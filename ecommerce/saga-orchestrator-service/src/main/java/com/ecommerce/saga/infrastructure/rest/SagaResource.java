package com.ecommerce.saga.infrastructure.rest;

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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.smallrye.mutiny.Uni;

import com.ecommerce.saga.application.service.SagaOrchestratorService;
import com.ecommerce.saga.application.service.SagaQueryService;
import com.ecommerce.saga.domain.model.enums.SagaState;
import com.ecommerce.saga.shared.requests.PageRequest;

/**
 * Superficie de operación/observabilidad de la saga — permite leer la
 * máquina de estados y la línea de tiempo de cualquier orden, y reenviar
 * manualmente el último comando de una saga atascada si una caída de algún
 * servicio se "tragó" el mensaje.
 */
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Sagas", description = "Saga instances — state machine + timeline for the order-to-delivery flow")
public class SagaResource {

    private final SagaQueryService sagaQueryService;
    private final SagaOrchestratorService sagaOrchestratorService;

    @Inject
    public SagaResource(SagaQueryService sagaQueryService, SagaOrchestratorService sagaOrchestratorService) {
        this.sagaQueryService = sagaQueryService;
        this.sagaOrchestratorService = sagaOrchestratorService;
    }

    @GET
    @Path("/{orderId}")
    @Operation(summary = "Get a saga's state and step timeline")
    @APIResponse(responseCode = "200", description = "Found")
    @APIResponse(responseCode = "404", description = "No saga for this order")
    public Uni<Response> getByOrderId(
        @Parameter(description = "Order id", required = true) @PathParam("orderId") UUID orderId) {
        return sagaQueryService.getByOrderId(orderId).map(response -> Response.ok(response).build());
    }

    @GET
    @Operation(summary = "List sagas", description = "Paged, optionally filtered by state.")
    @APIResponse(responseCode = "200", description = "Listing")
    public Uni<Response> list(@Valid @BeanParam PageRequest pageRequest,
        @QueryParam("state") SagaState state) {
        return sagaQueryService.listPaged(pageRequest.getPage(), pageRequest.getLimit(), state)
            .map(paged -> Response.ok(paged).build());
    }

    @POST
    @Path("/{orderId}/retry")
    @Operation(summary = "Re-publish a stuck saga's last command",
        description = "Recovery tool: re-sends the exact payload of the most recent outbound "
            + "command (same embedded eventId) so downstream idempotency decides whether it's a "
            + "genuine retry or a no-op.")
    @APIResponse(responseCode = "202", description = "Retry accepted")
    @APIResponse(responseCode = "404", description = "No saga for this order")
    public Uni<Response> retry(
        @Parameter(description = "Order id", required = true) @PathParam("orderId") UUID orderId) {
        return sagaQueryService.getByOrderId(orderId)
            .chain(() -> sagaOrchestratorService.retryLastCommand(orderId))
            .map(v -> Response.accepted().build());
    }
}
