package com.ecommerce.payment.infrastructure.rest;

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

import com.ecommerce.payment.domain.model.enums.PaymentStatus;
import com.ecommerce.payment.application.service.PaymentService;
import com.ecommerce.payment.shared.requests.PageRequest;

/** Solo lectura — los pagos únicamente se crean o actualizan mediante los comandos de Kafka ChargePayment/RefundPayment. */
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Payments", description = "Payment records, driven entirely by saga commands")
public class PaymentResource {

    private final PaymentService paymentService;

    @Inject
    public PaymentResource(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GET
    @Path("/{orderId}")
    @Operation(summary = "Get the payment for an order")
    @APIResponse(responseCode = "200", description = "Found")
    @APIResponse(responseCode = "404", description = "No payment recorded for this order")
    public Uni<Response> getByOrderId(
        @Parameter(description = "Order id", required = true) @PathParam("orderId") UUID orderId) {
        return paymentService.getByOrderId(orderId).map(response -> Response.ok(response).build());
    }

    @GET
    @Operation(summary = "List payments", description = "Paged, optionally filtered by status.")
    @APIResponse(responseCode = "200", description = "Listing")
    public Uni<Response> list(@Valid @BeanParam PageRequest pageRequest,
        @QueryParam("status") PaymentStatus status) {
        return paymentService.listPaged(pageRequest.getPage(), pageRequest.getLimit(), status)
            .map(paged -> Response.ok(paged).build());
    }
}
