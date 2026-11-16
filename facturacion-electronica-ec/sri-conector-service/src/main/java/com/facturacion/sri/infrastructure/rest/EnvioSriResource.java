package com.facturacion.sri.infrastructure.rest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import jakarta.inject.Inject;
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

import com.facturacion.commons.security.TokenPrincipal;
import com.facturacion.sri.application.service.SriConectorService;
import com.facturacion.sri.domain.model.entity.EnvioSri;

/**
 * Superficie de observabilidad — ver el estado del envío/autorización de un
 * comprobante ante el SRI. Nada aquí modifica estado: eso lo hace
 * exclusivamente {@code EnviarComprobanteCommandConsumer}/{@code AutorizacionPoller}.
 */
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Envíos SRI", description = "Estado del envío/autorización de un comprobante ante el SRI real")
public class EnvioSriResource {

    private final SriConectorService sriConectorService;
    private final TokenPrincipal tokenPrincipal;

    @Inject
    public EnvioSriResource(SriConectorService sriConectorService, TokenPrincipal tokenPrincipal) {
        this.sriConectorService = sriConectorService;
        this.tokenPrincipal = tokenPrincipal;
    }

    @GET
    @Path("/envios/comprobante/{comprobanteId}")
    @Operation(summary = "Estado del envío al SRI de un comprobante, por su id en comprobantes-service")
    @APIResponse(responseCode = "200", description = "Encontrado")
    @APIResponse(responseCode = "404", description = "Todavía no se envió (o nunca se enviará) este comprobante al SRI")
    public Uni<Response> obtenerPorComprobanteId(
            @Parameter(description = "Id del comprobante en comprobantes-service", required = true)
            @PathParam("comprobanteId") UUID comprobanteId) {
        return sriConectorService.obtenerPorComprobanteId(comprobanteId, tokenPrincipal.getRuc())
            .map(response -> Response.ok(response).build());
    }

    @GET
    @Path("/envios/clave/{claveAcceso}")
    @Operation(summary = "Estado del envío al SRI, por clave de acceso")
    @APIResponse(responseCode = "200", description = "Encontrado")
    @APIResponse(responseCode = "404", description = "No existe un envío con esa clave de acceso")
    public Uni<Response> obtenerPorClaveAcceso(
            @Parameter(description = "Clave de acceso de 49 dígitos", required = true)
            @PathParam("claveAcceso") String claveAcceso) {
        return sriConectorService.obtenerPorClaveAcceso(claveAcceso, tokenPrincipal.getRuc())
            .map(response -> Response.ok(response).build());
    }

    @GET
    @Path("/envios/comprobante/{comprobanteId}/xml")
    @Produces(MediaType.APPLICATION_XML)
    @Operation(summary = "Descargar el XML asociado a este envío",
        description = "tipo=firmado (lo que se mandó a Recepción) o autorizado (el bloque <autorizacion> completo devuelto por el SRI, no solo el comprobante) — 404 si esa etapa todavía no ocurrió.")
    @APIResponse(responseCode = "200", description = "El XML de esa etapa")
    @APIResponse(responseCode = "404", description = "No encontrado, o esa etapa todavía no ocurrió")
    public Uni<Response> obtenerXml(
            @Parameter(description = "Id del comprobante en comprobantes-service", required = true)
            @PathParam("comprobanteId") UUID comprobanteId,
            @Parameter(description = "firmado | autorizado")
            @QueryParam("tipo") @jakarta.ws.rs.DefaultValue("firmado") String tipo) {
        return sriConectorService.obtenerDominioPorComprobanteId(comprobanteId, tokenPrincipal.getRuc()).map((EnvioSri envio) -> {
            String xml = "autorizado".equals(tipo) ? envio.getXmlAutorizado() : envio.getXmlFirmado();
            if (xml == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(xml.getBytes(StandardCharsets.UTF_8)).build();
        });
    }
}
