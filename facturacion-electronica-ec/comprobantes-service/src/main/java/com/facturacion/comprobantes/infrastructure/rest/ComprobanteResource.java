package com.facturacion.comprobantes.infrastructure.rest;

import java.net.URI;
import java.nio.charset.StandardCharsets;
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

import com.facturacion.comprobantes.application.dto.ComprobanteResponse;
import com.facturacion.comprobantes.application.dto.CrearFacturaRequest;
import com.facturacion.comprobantes.application.dto.CrearGuiaRemisionRequest;
import com.facturacion.comprobantes.application.dto.CrearLiquidacionCompraRequest;
import com.facturacion.comprobantes.application.dto.CrearNotaCreditoRequest;
import com.facturacion.comprobantes.application.dto.CrearNotaDebitoRequest;
import com.facturacion.comprobantes.application.dto.CrearRetencionRequest;
import com.facturacion.comprobantes.application.service.ComprobanteService;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.enums.EstadoComprobante;
import com.facturacion.comprobantes.infrastructure.security.TokenPrincipal;
import com.facturacion.comprobantes.shared.exception.SinPermisoException;
import com.facturacion.comprobantes.shared.requests.PageRequest;
import com.facturacion.comprobantes.shared.responses.PagedResponse;

/**
 * Superficie de observabilidad y de creación — igual que en ecommerce/,
 * nada aquí modifica el estado de un comprobante una vez creado: eso
 * siempre ocurre vía los comandos que manda facturacion-orchestrator-service
 * por Kafka. Cada creación valida que el {@code ruc} del emisor coincida
 * con el de la api-key usada (ver ApiKeyAuthFilter) — una key solo puede
 * emitir para el ruc al que está asociada.
 */
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Comprobantes", description = "Comprobantes electrónicos — Factura, Nota de Crédito, Nota de Débito, Guía de Remisión, Retención y Liquidación de Compra")
public class ComprobanteResource {

    private final ComprobanteService comprobanteService;
    private final TokenPrincipal tokenPrincipal;

    @Inject
    public ComprobanteResource(ComprobanteService comprobanteService, TokenPrincipal tokenPrincipal) {
        this.comprobanteService = comprobanteService;
        this.tokenPrincipal = tokenPrincipal;
    }

    private void validarRuc(String rucRequest) {
        if (!tokenPrincipal.getRuc().equals(rucRequest)) {
            throw new SinPermisoException(
                "La api-key usada es para el ruc " + tokenPrincipal.getRuc() + ", no para " + rucRequest);
        }
    }

    @POST
    @Path("/facturas")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Crear una factura", description = "Genera la clave de acceso y el XML sin firmar, y publica ComprobanteCreado — arranca la saga.")
    @APIResponse(responseCode = "201", description = "Factura creada en estado BORRADOR")
    @APIResponse(responseCode = "400", description = "Solicitud inválida")
    @APIResponse(responseCode = "403", description = "La api-key no es del ruc del emisor")
    public Uni<Response> crearFactura(@Valid CrearFacturaRequest request, @Context UriInfo uriInfo) {
        validarRuc(request.getEmisor().getRuc());
        return comprobanteService.crearFactura(request)
            .map(response -> {
                URI location = uriInfo.getBaseUriBuilder().path("comprobantes").path(response.getId().toString())
                    .build();
                return Response.created(location).entity(response).build();
            });
    }

    @POST
    @Path("/notas-credito")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Crear una nota de crédito", description = "Modifica (total o parcialmente) un comprobante ya emitido.")
    @APIResponse(responseCode = "201", description = "Nota de crédito creada en estado BORRADOR")
    @APIResponse(responseCode = "400", description = "Solicitud inválida")
    @APIResponse(responseCode = "403", description = "La api-key no es del ruc del emisor")
    public Uni<Response> crearNotaCredito(@Valid CrearNotaCreditoRequest request, @Context UriInfo uriInfo) {
        validarRuc(request.getEmisor().getRuc());
        return comprobanteService.crearNotaCredito(request)
            .map(response -> {
                URI location = uriInfo.getBaseUriBuilder().path("comprobantes").path(response.getId().toString())
                    .build();
                return Response.created(location).entity(response).build();
            });
    }

    @POST
    @Path("/notas-debito")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Crear una nota de débito", description = "Cobra un valor adicional sobre un comprobante ya emitido (intereses, mora, etc.).")
    @APIResponse(responseCode = "201", description = "Nota de débito creada en estado BORRADOR")
    @APIResponse(responseCode = "400", description = "Solicitud inválida")
    @APIResponse(responseCode = "403", description = "La api-key no es del ruc del emisor")
    public Uni<Response> crearNotaDebito(@Valid CrearNotaDebitoRequest request, @Context UriInfo uriInfo) {
        validarRuc(request.getEmisor().getRuc());
        return comprobanteService.crearNotaDebito(request)
            .map(response -> {
                URI location = uriInfo.getBaseUriBuilder().path("comprobantes").path(response.getId().toString())
                    .build();
                return Response.created(location).entity(response).build();
            });
    }

    @POST
    @Path("/guias-remision")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Crear una guía de remisión", description = "Traslada bienes a uno o más destinatarios — no es una venta, no lleva comprador ni valores.")
    @APIResponse(responseCode = "201", description = "Guía de remisión creada en estado BORRADOR")
    @APIResponse(responseCode = "400", description = "Solicitud inválida")
    @APIResponse(responseCode = "403", description = "La api-key no es del ruc del emisor")
    public Uni<Response> crearGuiaRemision(@Valid CrearGuiaRemisionRequest request, @Context UriInfo uriInfo) {
        validarRuc(request.getEmisor().getRuc());
        return comprobanteService.crearGuiaRemision(request)
            .map(response -> {
                URI location = uriInfo.getBaseUriBuilder().path("comprobantes").path(response.getId().toString())
                    .build();
                return Response.created(location).entity(response).build();
            });
    }

    @POST
    @Path("/retenciones")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Crear un comprobante de retención", description = "Certifica la retención de IVA/renta aplicada sobre uno o más documentos sustento.")
    @APIResponse(responseCode = "201", description = "Retención creada en estado BORRADOR")
    @APIResponse(responseCode = "400", description = "Solicitud inválida")
    @APIResponse(responseCode = "403", description = "La api-key no es del ruc del emisor")
    public Uni<Response> crearRetencion(@Valid CrearRetencionRequest request, @Context UriInfo uriInfo) {
        validarRuc(request.getEmisor().getRuc());
        return comprobanteService.crearRetencion(request)
            .map(response -> {
                URI location = uriInfo.getBaseUriBuilder().path("comprobantes").path(response.getId().toString())
                    .build();
                return Response.created(location).entity(response).build();
            });
    }

    @POST
    @Path("/liquidaciones-compra")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Crear una liquidación de compra", description = "El propio comprador emite el comprobante en nombre del proveedor, cuando este no puede emitir el suyo.")
    @APIResponse(responseCode = "201", description = "Liquidación de compra creada en estado BORRADOR")
    @APIResponse(responseCode = "400", description = "Solicitud inválida")
    @APIResponse(responseCode = "403", description = "La api-key no es del ruc del emisor")
    public Uni<Response> crearLiquidacionCompra(@Valid CrearLiquidacionCompraRequest request,
            @Context UriInfo uriInfo) {
        validarRuc(request.getEmisor().getRuc());
        return comprobanteService.crearLiquidacionCompra(request)
            .map(response -> {
                URI location = uriInfo.getBaseUriBuilder().path("comprobantes").path(response.getId().toString())
                    .build();
                return Response.created(location).entity(response).build();
            });
    }

    @GET
    @Path("/comprobantes/{id}")
    @Operation(summary = "Obtener un comprobante por id")
    @APIResponse(responseCode = "200", description = "Encontrado")
    @APIResponse(responseCode = "404", description = "Comprobante no encontrado")
    public Uni<Response> obtenerPorId(
            @Parameter(description = "Id del comprobante", required = true) @PathParam("id") UUID id) {
        return comprobanteService.obtenerPorId(id).map(response -> Response.ok(response).build());
    }

    @GET
    @Path("/comprobantes")
    @Operation(summary = "Listar comprobantes", description = "Paginado, con filtro opcional por estado y/o emisor.")
    @APIResponse(responseCode = "200", description = "Listado")
    public Uni<Response> listar(@Valid @BeanParam PageRequest pageRequest,
            @QueryParam("estado") EstadoComprobante estado, @QueryParam("emisorRuc") String emisorRuc) {
        return comprobanteService.listarPaginado(pageRequest.getPage(), pageRequest.getLimit(), estado, emisorRuc)
            .map((PagedResponse<ComprobanteResponse> paged) -> Response.ok(paged).build());
    }

    @GET
    @Path("/comprobantes/{id}/xml")
    @Produces(MediaType.APPLICATION_XML)
    @Operation(summary = "Descargar el XML del comprobante en una etapa específica",
        description = "tipo=generado (sin firmar, siempre existe), firmado (tras firma-electronica-service) o autorizado (tras el SRI) — 404 si esa etapa todavía no ocurrió.")
    @APIResponse(responseCode = "200", description = "El XML de esa etapa")
    @APIResponse(responseCode = "404", description = "Comprobante no encontrado, o esa etapa todavía no ocurrió")
    public Uni<Response> obtenerXml(
            @Parameter(description = "Id del comprobante", required = true) @PathParam("id") UUID id,
            @Parameter(description = "generado | firmado | autorizado")
            @QueryParam("tipo") @jakarta.ws.rs.DefaultValue("generado") String tipo) {
        return comprobanteService.obtenerDominioPorId(id).map((Comprobante c) -> {
            String xml = switch (tipo) {
                case "firmado" -> c.getXmlFirmado();
                case "autorizado" -> c.getXmlAutorizado();
                default -> c.getXmlGenerado();
            };
            if (xml == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(xml.getBytes(StandardCharsets.UTF_8)).build();
        });
    }

    @GET
    @Path("/comprobantes/{id}/ride")
    @Produces("application/pdf")
    @Operation(summary = "Descargar el RIDE (representación impresa) en PDF",
        description = "Solo existe una vez que el comprobante está AUTORIZADO.")
    @APIResponse(responseCode = "200", description = "El RIDE en PDF")
    @APIResponse(responseCode = "404", description = "Comprobante no encontrado, o todavía no está autorizado")
    public Uni<Response> obtenerRide(
            @Parameter(description = "Id del comprobante", required = true) @PathParam("id") UUID id) {
        return comprobanteService.obtenerDominioPorId(id).map((Comprobante c) -> {
            if (c.getRidePdf() == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(c.getRidePdf())
                .header("Content-Disposition", "inline; filename=\"" + c.getClaveAcceso() + ".pdf\"")
                .build();
        });
    }
}
