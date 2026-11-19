package com.facturacion.comprobantes.infrastructure.rest;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.smallrye.mutiny.Uni;

import com.facturacion.commons.rest.Respuestas;
import com.facturacion.commons.paging.PageRequest;
import com.facturacion.commons.paging.PagedResponse;
import com.facturacion.commons.security.TokenPrincipal;
import com.facturacion.comprobantes.application.dto.ComprobanteResponse;
import com.facturacion.comprobantes.application.dto.CrearFacturaRequest;
import com.facturacion.comprobantes.application.dto.CrearGuiaRemisionRequest;
import com.facturacion.comprobantes.application.dto.CrearLiquidacionCompraRequest;
import com.facturacion.comprobantes.application.dto.CrearNotaCreditoRequest;
import com.facturacion.comprobantes.application.dto.CrearNotaDebitoRequest;
import com.facturacion.comprobantes.application.dto.CrearRetencionRequest;
import com.facturacion.comprobantes.application.dto.EmpresaRequest;
import com.facturacion.comprobantes.application.dto.ResultadoEmision;
import com.facturacion.comprobantes.application.service.ComprobanteConsultaService;
import com.facturacion.comprobantes.application.service.ComprobanteService;
import com.facturacion.comprobantes.application.service.ReenvioNotificacionService;
import com.facturacion.comprobantes.application.service.TicketService;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.enums.EstadoComprobante;
import com.facturacion.comprobantes.domain.model.enums.FormatoTicket;
import com.facturacion.comprobantes.domain.model.vo.AnchoTicket;
import com.facturacion.comprobantes.shared.constant.ApiConstants;

/**
 * Superficie de observabilidad y de creación:
 * nada aquí modifica el estado de un comprobante una vez creado: eso
 * siempre ocurre vía los comandos que manda facturacion-orchestrator-service
 * por Kafka. Cada creación valida que el {@code ruc} de la empresa coincida
 * con el de la api-key usada (ver ApiKeyAuthFilter) — una key solo puede
 * emitir para el ruc al que está asociada.
 */
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Comprobantes", description = "Comprobantes electrónicos — Factura, Nota de Crédito, Nota de Débito, Guía de Remisión, Retención y Liquidación de Compra")
public class ComprobanteResource {

    /** Parte de la cabecera Content-Disposition del ticket: nombre del archivo con la clave de acceso y la extensión. */
    private static final String NOMBRE_ARCHIVO = "; filename=\"ticket-%s%s\"";

    private static final String DESCRIPCION_CLAVE = "Clave única por comprobante (8 a 128 caracteres: letras, números y . _ : -). "
        + "Si reintentas la petición con la misma clave y el mismo cuerpo, recibes el comprobante ya creado (cabecera "
        + "Idempotency-Replayed: true) en vez de uno nuevo; con otro cuerpo, 422.";

    private final ComprobanteService comprobanteService;
    private final ComprobanteConsultaService consultaService;
    private final ReenvioNotificacionService reenvioService;
    private final TicketService ticketService;
    private final TokenPrincipal tokenPrincipal;

    @Inject
    public ComprobanteResource(ComprobanteService comprobanteService, ComprobanteConsultaService consultaService,
            ReenvioNotificacionService reenvioService, TicketService ticketService, TokenPrincipal tokenPrincipal) {
        this.comprobanteService = comprobanteService;
        this.consultaService = consultaService;
        this.reenvioService = reenvioService;
        this.ticketService = ticketService;
        this.tokenPrincipal = tokenPrincipal;
    }

    /**
     * Valida ruc (como antes) y, si la api-key viene acotada a un
     * establecimiento/punto de emisión específico (ver
     * {@code identidad-service.ApiKeyService.crear}), que el request coincida
     * exactamente — una api-key sin esa restricción (el caso normal,
     * {@code tokenPrincipal.getEstablecimiento() == null}) no se ve afectada.
     */
    private void validarAlcance(EmpresaRequest empresa) {
        tokenPrincipal.exigirEscritura();
        tokenPrincipal.exigirAlcance(empresa.getRuc(), empresa.getEstablecimiento(), empresa.getPuntoEmision());
    }

    /**
     * Arma el 201 de una emisión con su {@code Location}. Si es la respuesta de una solicitud anterior con la misma clave de
     * idempotencia, agrega la cabecera {@code Idempotency-Replayed: true}.
     */
    private Response armarRespuestaCreacion(ResultadoEmision resultado, UriInfo uriInfo) {
        URI location = uriInfo.getBaseUriBuilder().path("comprobantes").path(resultado.comprobante().getId().toString())
            .build();
        Response respuesta = Respuestas.creado(location, resultado.comprobante());
        return resultado.repetida()
            ? Response.fromResponse(respuesta).header(ApiConstants.HEADER_EMISION_REPETIDA, Boolean.TRUE).build()
            : respuesta;
    }

    @POST
    @Path("/facturas")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Crear una factura", description = "Genera la clave de acceso y el XML sin firmar, y publica ComprobanteCreado — arranca la saga.")
    @APIResponse(responseCode = "201", description = "Factura creada en estado BORRADOR")
    @APIResponse(responseCode = "400", description = "Solicitud inválida")
    @APIResponse(responseCode = "403", description = "La api-key no es del ruc de la empresa")
    public Uni<Response> crearFactura(@Valid CrearFacturaRequest request,
            @HeaderParam(ApiConstants.HEADER_CLAVE_IDEMPOTENCIA) @Parameter(description = DESCRIPCION_CLAVE) String claveIdempotencia,
            @Context UriInfo uriInfo) {
        validarAlcance(request.getEmpresa());
        return comprobanteService.crearFactura(request, claveIdempotencia)
            .map(resultado -> armarRespuestaCreacion(resultado, uriInfo));
    }

    @POST
    @Path("/notas-credito")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Crear una nota de crédito", description = "Modifica (total o parcialmente) un comprobante ya emitido.")
    @APIResponse(responseCode = "201", description = "Nota de crédito creada en estado BORRADOR")
    @APIResponse(responseCode = "400", description = "Solicitud inválida")
    @APIResponse(responseCode = "403", description = "La api-key no es del ruc de la empresa")
    public Uni<Response> crearNotaCredito(@Valid CrearNotaCreditoRequest request,
            @HeaderParam(ApiConstants.HEADER_CLAVE_IDEMPOTENCIA) @Parameter(description = DESCRIPCION_CLAVE) String claveIdempotencia,
            @Context UriInfo uriInfo) {
        validarAlcance(request.getEmpresa());
        return comprobanteService.crearNotaCredito(request, claveIdempotencia)
            .map(resultado -> armarRespuestaCreacion(resultado, uriInfo));
    }

    @POST
    @Path("/notas-debito")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Crear una nota de débito", description = "Cobra un valor adicional sobre un comprobante ya emitido (intereses, mora, etc.).")
    @APIResponse(responseCode = "201", description = "Nota de débito creada en estado BORRADOR")
    @APIResponse(responseCode = "400", description = "Solicitud inválida")
    @APIResponse(responseCode = "403", description = "La api-key no es del ruc de la empresa")
    public Uni<Response> crearNotaDebito(@Valid CrearNotaDebitoRequest request,
            @HeaderParam(ApiConstants.HEADER_CLAVE_IDEMPOTENCIA) @Parameter(description = DESCRIPCION_CLAVE) String claveIdempotencia,
            @Context UriInfo uriInfo) {
        validarAlcance(request.getEmpresa());
        return comprobanteService.crearNotaDebito(request, claveIdempotencia)
            .map(resultado -> armarRespuestaCreacion(resultado, uriInfo));
    }

    @POST
    @Path("/guias-remision")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Crear una guía de remisión", description = "Traslada bienes a uno o más destinatarios — no es una venta, no lleva comprador ni valores.")
    @APIResponse(responseCode = "201", description = "Guía de remisión creada en estado BORRADOR")
    @APIResponse(responseCode = "400", description = "Solicitud inválida")
    @APIResponse(responseCode = "403", description = "La api-key no es del ruc de la empresa")
    public Uni<Response> crearGuiaRemision(@Valid CrearGuiaRemisionRequest request,
            @HeaderParam(ApiConstants.HEADER_CLAVE_IDEMPOTENCIA) @Parameter(description = DESCRIPCION_CLAVE) String claveIdempotencia,
            @Context UriInfo uriInfo) {
        validarAlcance(request.getEmpresa());
        return comprobanteService.crearGuiaRemision(request, claveIdempotencia)
            .map(resultado -> armarRespuestaCreacion(resultado, uriInfo));
    }

    @POST
    @Path("/retenciones")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Crear un comprobante de retención", description = "Certifica la retención de IVA/renta aplicada sobre uno o más documentos sustento.")
    @APIResponse(responseCode = "201", description = "Retención creada en estado BORRADOR")
    @APIResponse(responseCode = "400", description = "Solicitud inválida")
    @APIResponse(responseCode = "403", description = "La api-key no es del ruc de la empresa")
    public Uni<Response> crearRetencion(@Valid CrearRetencionRequest request,
            @HeaderParam(ApiConstants.HEADER_CLAVE_IDEMPOTENCIA) @Parameter(description = DESCRIPCION_CLAVE) String claveIdempotencia,
            @Context UriInfo uriInfo) {
        validarAlcance(request.getEmpresa());
        return comprobanteService.crearRetencion(request, claveIdempotencia)
            .map(resultado -> armarRespuestaCreacion(resultado, uriInfo));
    }

    @POST
    @Path("/liquidaciones-compra")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Crear una liquidación de compra", description = "El propio comprador emite el comprobante en nombre del proveedor, cuando este no puede emitir el suyo.")
    @APIResponse(responseCode = "201", description = "Liquidación de compra creada en estado BORRADOR")
    @APIResponse(responseCode = "400", description = "Solicitud inválida")
    @APIResponse(responseCode = "403", description = "La api-key no es del ruc de la empresa")
    public Uni<Response> crearLiquidacionCompra(@Valid CrearLiquidacionCompraRequest request,
            @HeaderParam(ApiConstants.HEADER_CLAVE_IDEMPOTENCIA) @Parameter(description = DESCRIPCION_CLAVE) String claveIdempotencia,
            @Context UriInfo uriInfo) {
        validarAlcance(request.getEmpresa());
        return comprobanteService.crearLiquidacionCompra(request, claveIdempotencia)
            .map(resultado -> armarRespuestaCreacion(resultado, uriInfo));
    }

    @GET
    @Path("/comprobantes/{id}")
    @Operation(summary = "Obtener un comprobante por id")
    @APIResponse(responseCode = "200", description = "Encontrado")
    @APIResponse(responseCode = "404", description = "Comprobante no encontrado")
    public Uni<Response> obtenerPorId(
            @Parameter(description = "Id del comprobante", required = true) @PathParam("id") UUID id) {
        return consultaService.obtenerPorId(id, tokenPrincipal.getRuc()).map(response -> Response.ok(response).build());
    }

    @GET
    @Path("/comprobantes")
    @Operation(summary = "Listar comprobantes", description = "Paginado, con filtro opcional por estado y/o empresa.")
    @APIResponse(responseCode = "200", description = "Listado")
    public Uni<Response> listar(@Valid @BeanParam PageRequest pageRequest,
            @QueryParam("estado") EstadoComprobante estado, @QueryParam("empresaRuc") String empresaRuc) {
        // El listado es siempre de la empresa de la api-key; pedir el de otro RUC es un error, no un resultado vacío.
        if (empresaRuc != null) {
            tokenPrincipal.exigirEmpresa(empresaRuc);
        }
        return consultaService.listarPaginado(pageRequest.getPage(), pageRequest.getLimit(), estado,
                tokenPrincipal.getRuc())
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
        return consultaService.obtenerDominioPorId(id, tokenPrincipal.getRuc()).map((Comprobante c) -> {
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
        return consultaService.obtenerDominioPorId(id, tokenPrincipal.getRuc()).map((Comprobante c) -> {
            if (c.getRidePdf() == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(c.getRidePdf())
                .header("Content-Disposition", "inline; filename=\"" + c.getClaveAcceso() + ".pdf\"")
                .build();
        });
    }

    @GET
    @Path("/comprobantes/{id}/ticket")
    @Produces({FormatoTicket.TIPO_TEXTO, FormatoTicket.TIPO_ESCPOS})
    @Operation(summary = "Ticket (recibo corto) para impresora de recibos",
        description = "Datos mínimos para entregar al cliente: emisor, número, fecha, cliente, líneas, totales, forma de pago y "
            + "estado de autorización con la clave de acceso. formato=texto (por defecto, texto plano ya ajustado al papel) "
            + "o escpos (bytes ESC/POS para enviar tal cual a la impresora, con negrita y corte; solo ASCII). "
            + "ancho = milímetros del papel configurados en ticket.anchos (por defecto 58 y 80). Se puede pedir en cualquier estado; no reemplaza al RIDE. "
            + "Disponible para factura, nota de crédito, nota de débito y liquidación de compra.")
    @APIResponse(responseCode = "200", description = "El ticket")
    @APIResponse(responseCode = "400", description = "Formato o ancho inválido, o tipo de comprobante sin ticket")
    @APIResponse(responseCode = "404", description = "Comprobante no encontrado")
    public Uni<Response> obtenerTicket(
            @Parameter(description = "Id del comprobante", required = true) @PathParam("id") UUID id,
            @Parameter(description = "texto | escpos") @QueryParam(FormatoTicket.PARAMETRO) @DefaultValue(FormatoTicket.POR_DEFECTO) String formato,
            @Parameter(description = "Ancho del papel en mm; debe estar en ticket.anchos (por defecto ticket.ancho-por-defecto)") @QueryParam(AnchoTicket.PARAMETRO) Integer ancho) {
        return ticketService.generar(id, tokenPrincipal.getRuc(), formato, ancho).map(ticket -> Response
            .ok(ticket.contenido(), ticket.formato().tipoContenido())
            .header(HttpHeaders.CONTENT_DISPOSITION, ticket.formato().disposicion() + NOMBRE_ARCHIVO.formatted(
                ticket.claveAcceso(), ticket.formato().extension()))
            .build());
    }

    @POST
    @Path("/comprobantes/{id}/reenviar-correo")
    @Operation(summary = "Reenviar el correo del comprobante",
        description = "Vuelve a enviar al receptor el RIDE y el XML autorizado, p. ej. cuando el primer envío falló "
            + "(NOTIFICACION_FALLIDA). Es asíncrono: el resultado queda en GET /notificaciones/comprobante/{id}.")
    @APIResponse(responseCode = "202", description = "Reenvío aceptado")
    @APIResponse(responseCode = "404", description = "Comprobante no encontrado")
    @APIResponse(responseCode = "409", description = "No está autorizado, no tiene RIDE o no tiene correo del receptor")
    public Uni<Response> reenviarCorreo(
            @Parameter(description = "Id del comprobante", required = true) @PathParam("id") UUID id) {
        tokenPrincipal.exigirEscritura();
        return reenvioService.reenviarCorreo(id, tokenPrincipal.getRuc()).map(v -> Respuestas.aceptado());
    }
}
