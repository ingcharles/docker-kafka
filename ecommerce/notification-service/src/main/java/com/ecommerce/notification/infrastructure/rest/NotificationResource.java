package com.ecommerce.notification.infrastructure.rest;

import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.smallrye.mutiny.Uni;

import com.ecommerce.notification.application.dto.NotificationResponse;
import com.ecommerce.notification.application.service.NotificationQueryService;
import com.ecommerce.notification.shared.requests.PageRequest;

/** Solo lectura — las notificaciones únicamente se crean mediante el comando de Kafka SendNotification. */
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Notifications", description = "Notification delivery log, driven entirely by saga commands")
public class NotificationResource {

    private final NotificationQueryService notificationQueryService;

    @Inject
    public NotificationResource(NotificationQueryService notificationQueryService) {
        this.notificationQueryService = notificationQueryService;
    }

    @GET
    @Path("/{orderId}")
    @Operation(summary = "List notifications sent for an order")
    @APIResponse(responseCode = "200", description = "Listing (possibly empty)")
    public Uni<Response> listByOrderId(
        @Parameter(description = "Order id", required = true) @PathParam("orderId") UUID orderId) {
        return notificationQueryService.listByOrderId(orderId)
            .map((List<NotificationResponse> list) -> Response.ok(list).build());
    }

    @GET
    @Operation(summary = "List notifications", description = "Paged, newest first.")
    @APIResponse(responseCode = "200", description = "Listing")
    public Uni<Response> list(@Valid @BeanParam PageRequest pageRequest) {
        return notificationQueryService.listPaged(pageRequest.getPage(), pageRequest.getLimit())
            .map(paged -> Response.ok(paged).build());
    }
}
