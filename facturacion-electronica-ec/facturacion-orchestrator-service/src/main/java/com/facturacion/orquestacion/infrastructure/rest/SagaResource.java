package com.facturacion.orquestacion.infrastructure.rest;

import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
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

import com.facturacion.commons.rest.Respuestas;
import com.facturacion.commons.paging.PageRequest;
import com.facturacion.commons.security.TokenPrincipal;
import com.facturacion.orquestacion.application.dto.ReenvioMasivoResponse;
import com.facturacion.orquestacion.application.service.SagaOrchestratorService;
import com.facturacion.orquestacion.application.service.SagaQueryService;
import com.facturacion.orquestacion.domain.model.enums.SagaState;

/**
 * Superficie de operación/observabilidad de la saga — permite leer la
 * máquina de estados y la línea de tiempo de cualquier comprobante, y
 * reenviar manualmente el último comando de una saga atascada si una caída
 * de algún servicio se "tragó" el mensaje.
 */
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Sagas", description = "Instancias de saga — máquina de estados + línea de tiempo del flujo crear→firmar→enviar→autorizar→notificar")
public class SagaResource {

    private final SagaQueryService sagaQueryService;
    private final SagaOrchestratorService sagaOrchestratorService;
    private final TokenPrincipal tokenPrincipal;

    @Inject
    public SagaResource(SagaQueryService sagaQueryService, SagaOrchestratorService sagaOrchestratorService,
            TokenPrincipal tokenPrincipal) {
        this.sagaQueryService = sagaQueryService;
        this.sagaOrchestratorService = sagaOrchestratorService;
        this.tokenPrincipal = tokenPrincipal;
    }

    @GET
    @Path("/{comprobanteId}")
    @Operation(summary = "Estado y línea de tiempo de la saga de un comprobante")
    @APIResponse(responseCode = "200", description = "Encontrada")
    @APIResponse(responseCode = "404", description = "No hay saga para ese comprobante")
    public Uni<Response> getByComprobanteId(
            @Parameter(description = "Id del comprobante", required = true) @PathParam("comprobanteId") UUID comprobanteId) {
        return sagaQueryService.getByComprobanteId(comprobanteId, tokenPrincipal.getRuc())
            .map(response -> Response.ok(response).build());
    }

    @GET
    @Operation(summary = "Listar sagas", description = "Paginado, con filtro opcional por estado. Solo las sagas de la empresa de la api-key.")
    @APIResponse(responseCode = "200", description = "Listado")
    public Uni<Response> list(@Valid @BeanParam PageRequest pageRequest, @QueryParam("state") SagaState state) {
        return sagaQueryService.listPaged(pageRequest.getPage(), pageRequest.getLimit(), state, tokenPrincipal.getRuc())
            .map(paged -> Response.ok(paged).build());
    }

    @POST
    @Path("/{comprobanteId}/retry")
    @Operation(summary = "Reenviar el último comando de una saga atascada",
        description = "Herramienta de recuperación: vuelve a publicar el payload exacto del último comando "
            + "saliente (con el mismo eventId incrustado), para que la idempotencia del servicio destino "
            + "decida si es un reintento genuino o si no hace falta nada.")
    @APIResponse(responseCode = "202", description = "Reintento aceptado")
    @APIResponse(responseCode = "404", description = "No hay saga para ese comprobante")
    public Uni<Response> reintentar(
            @Parameter(description = "Id del comprobante", required = true) @PathParam("comprobanteId") UUID comprobanteId) {
        tokenPrincipal.exigirEscritura();
        return sagaQueryService.getByComprobanteId(comprobanteId, tokenPrincipal.getRuc())
            .chain(() -> sagaOrchestratorService.reintentarUltimoComando(comprobanteId))
            .map(v -> Respuestas.aceptado());
    }

    @POST
    @Path("/reenviar-notificaciones")
    @Operation(summary = "Reenviar en masa los correos fallidos",
        description = "Reenvía el correo de las sagas de la empresa que están en NOTIFICACION_FALLIDA, hasta `limite` "
            + "por llamada (las demás se reenvían en la siguiente). Es asíncrono: cada una pasa a NOTIFICADO si el "
            + "envío sale bien, o vuelve a NOTIFICACION_FALLIDA con el motivo nuevo.")
    @APIResponse(responseCode = "202", description = "Cuántas sagas se mandaron a reenviar")
    public Uni<Response> reenviarNotificaciones(
            @Parameter(description = "Máximo de correos por llamada (1 a 500)")
            @QueryParam("limite") @DefaultValue("100") @Min(1) @Max(500) int limite) {
        tokenPrincipal.exigirEscritura();
        return sagaOrchestratorService.reenviarNotificacionesFallidas(tokenPrincipal.getRuc(), limite)
            .map(solicitados -> Respuestas.aceptado(new ReenvioMasivoResponse(solicitados)));
    }
}
