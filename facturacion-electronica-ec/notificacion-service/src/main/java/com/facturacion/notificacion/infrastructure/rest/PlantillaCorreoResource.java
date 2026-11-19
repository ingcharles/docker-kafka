package com.facturacion.notificacion.infrastructure.rest;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
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

import com.facturacion.commons.rest.Respuestas;
import com.facturacion.commons.security.TokenPrincipal;
import com.facturacion.notificacion.application.dto.PlantillaCorreoRequest;
import com.facturacion.notificacion.application.dto.VistaPreviaRequest;
import com.facturacion.notificacion.application.service.PlantillaCorreoService;

/**
 * Plantillas del correo que recibe el receptor, propias de cada empresa (la de la api-key: nunca se indica otro RUC). Leer es
 * para cualquier api-key de la empresa; guardar y eliminar, solo para el rol {@code ADMIN}. Sin plantilla propia el correo
 * sale con la plantilla por defecto del servicio.
 */
@Path("/plantillas-correo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Plantillas de correo", description = "Asunto y cuerpo (HTML y texto) del correo del comprobante, por empresa y tipo")
public class PlantillaCorreoResource {

    private static final String TIPO_DESCRIPCION =
        "FACTURA, NOTA_CREDITO, NOTA_DEBITO, GUIA_REMISION, RETENCION, LIQUIDACION_COMPRA o GENERICO (cualquier tipo sin plantilla propia)";

    private final PlantillaCorreoService plantillaService;
    private final TokenPrincipal tokenPrincipal;

    @Inject
    public PlantillaCorreoResource(PlantillaCorreoService plantillaService, TokenPrincipal tokenPrincipal) {
        this.plantillaService = plantillaService;
        this.tokenPrincipal = tokenPrincipal;
    }

    @GET
    @Operation(summary = "Las plantillas propias de la empresa")
    @APIResponse(responseCode = "200", description = "Las plantillas (puede ser ninguna)")
    public Uni<Response> listar() {
        return plantillaService.listar(tokenPrincipal.getRuc()).map(plantillas -> Response.ok(plantillas).build());
    }

    @GET
    @Path("/variables")
    @Operation(summary = "Variables que se pueden usar en una plantilla, escritas con dobles llaves")
    public Response listarVariables() {
        return Response.ok(plantillaService.listarVariables()).build();
    }

    @GET
    @Path("/por-defecto/{tipo}")
    @Operation(summary = "La plantilla por defecto de un tipo, con las variables sin reemplazar, como punto de partida")
    @APIResponse(responseCode = "200", description = "Asunto, cuerpo HTML (sin html/head/body) y cuerpo en texto")
    @APIResponse(responseCode = "400", description = "Tipo de comprobante inexistente")
    public Response obtenerPorDefecto(
            @Parameter(description = TIPO_DESCRIPCION, required = true) @PathParam("tipo") String tipo) {
        return Response.ok(plantillaService.obtenerPorDefecto(tipo)).build();
    }

    @POST
    @Path("/vista-previa")
    @Operation(summary = "Cómo quedaría el correo con esta plantilla, con datos de ejemplo y el logo real; no guarda nada")
    @APIResponse(responseCode = "200", description = "Asunto, HTML y texto armados")
    @APIResponse(responseCode = "400", description = "La plantilla no cumple las reglas (variable desconocida, variable dentro de una etiqueta...)")
    public Uni<Response> armarVistaPrevia(@Valid VistaPreviaRequest request) {
        return plantillaService.armarVistaPrevia(tokenPrincipal.getRuc(), request)
            .map(vista -> Response.ok(vista).build());
    }

    @GET
    @Path("/{tipo}")
    @Operation(summary = "La plantilla propia de la empresa para un tipo")
    @APIResponse(responseCode = "200", description = "La plantilla guardada (con el HTML ya limpiado)")
    @APIResponse(responseCode = "404", description = "La empresa no tiene plantilla propia para ese tipo")
    public Uni<Response> obtener(
            @Parameter(description = TIPO_DESCRIPCION, required = true) @PathParam("tipo") String tipo) {
        return plantillaService.obtener(tokenPrincipal.getRuc(), tipo).map(plantilla -> Response.ok(plantilla).build());
    }

    @PUT
    @Path("/{tipo}")
    @Operation(summary = "Crear o reemplazar la plantilla de la empresa para un tipo (rol ADMIN)",
        description = "El HTML se limpia al guardar: se quitan scripts, formularios, marcos, manejadores de eventos y "
            + "direcciones que no sean https, mailto o cid. Cada reemplazo sube la versión y deja la anterior en el historial.")
    @APIResponse(responseCode = "201", description = "Plantilla creada")
    @APIResponse(responseCode = "200", description = "Plantilla reemplazada")
    @APIResponse(responseCode = "400", description = "La plantilla no cumple las reglas")
    @APIResponse(responseCode = "403", description = "La api-key no es de rol ADMIN")
    public Uni<Response> guardar(
            @Parameter(description = TIPO_DESCRIPCION, required = true) @PathParam("tipo") String tipo,
            @Valid PlantillaCorreoRequest request, @Context UriInfo uriInfo) {
        tokenPrincipal.exigirAdmin();
        return plantillaService.guardar(tokenPrincipal.getRuc(), tipo, request)
            .map(resultado -> resultado.creada() ? Respuestas.creado(uriInfo.getAbsolutePath(), resultado.plantilla())
                : Respuestas.actualizado(resultado.plantilla()));
    }

    @DELETE
    @Path("/{tipo}")
    @Operation(summary = "Eliminar la plantilla propia de un tipo: el correo vuelve a la plantilla por defecto (rol ADMIN)")
    @APIResponse(responseCode = "204", description = "Eliminada; su última versión queda en el historial")
    @APIResponse(responseCode = "404", description = "La empresa no tiene plantilla propia para ese tipo")
    @APIResponse(responseCode = "403", description = "La api-key no es de rol ADMIN")
    public Uni<Response> eliminar(
            @Parameter(description = TIPO_DESCRIPCION, required = true) @PathParam("tipo") String tipo) {
        tokenPrincipal.exigirAdmin();
        return plantillaService.eliminar(tokenPrincipal.getRuc(), tipo).map(ignorado -> Respuestas.sinContenido());
    }

    @GET
    @Path("/{tipo}/historial")
    @Operation(summary = "Versiones anteriores de la plantilla, la más nueva primero")
    @APIResponse(responseCode = "200", description = "El historial (vacío si nunca cambió)")
    public Uni<Response> listarHistorial(
            @Parameter(description = TIPO_DESCRIPCION, required = true) @PathParam("tipo") String tipo) {
        return plantillaService.listarHistorial(tokenPrincipal.getRuc(), tipo)
            .map(versiones -> Response.ok(versiones).build());
    }
}
