package com.facturacion.firma.infrastructure.rest;

import java.net.URI;
import java.nio.file.Files;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
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
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.Vertx;

import com.facturacion.firma.application.dto.ActualizarEmisorRequest;
import com.facturacion.firma.application.dto.CertificadoMetadataResponse;
import com.facturacion.firma.application.dto.ConfiguracionEmisorResponse;
import com.facturacion.firma.application.dto.CrearEmisorRequest;
import com.facturacion.firma.application.dto.EmisorResponse;
import com.facturacion.firma.application.dto.EstablecerConfiguracionRequest;
import com.facturacion.firma.application.dto.SubirCertificadoRequest;
import com.facturacion.firma.application.service.CertificadoService;
import com.facturacion.firma.application.service.ConfiguracionEmisorService;
import com.facturacion.firma.application.service.EmisorService;
import com.facturacion.firma.shared.requests.PageRequest;
import com.facturacion.firma.shared.responses.PagedResponse;

/**
 * CRUD de emisores (multi-tenant) y carga del certificado de firma
 * electrónica. Es la única excepción a "cero REST entre servicios" del
 * stack: {@code comprobantes-service} y {@code sri-conector-service}
 * mantienen su propia copia de los datos de emisor vía los eventos
 * publicados en {@code facturacion.emisor.eventos.v1} (ver ARQUITECTURA.md)
 * — este REST es solo para el cliente/administrador externo.
 */
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Emisores", description = "Contribuyentes que emiten comprobantes electrónicos, y su certificado de firma")
public class EmisorResource {

    private final EmisorService emisorService;
    private final CertificadoService certificadoService;
    private final ConfiguracionEmisorService configuracionService;

    @Inject
    public EmisorResource(EmisorService emisorService, CertificadoService certificadoService,
            ConfiguracionEmisorService configuracionService) {
        this.emisorService = emisorService;
        this.certificadoService = certificadoService;
        this.configuracionService = configuracionService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Registrar un emisor", description = "Crea el contribuyente que emitirá comprobantes; el certificado se sube después con POST /{id}/certificado.")
    @APIResponse(responseCode = "201", description = "Emisor creado")
    @APIResponse(responseCode = "400", description = "Solicitud inválida")
    @APIResponse(responseCode = "409", description = "Ya existe un emisor con ese RUC")
    public Uni<Response> crear(@Valid CrearEmisorRequest request, @Context UriInfo uriInfo) {
        return emisorService.crearEmisor(request)
            .map(response -> {
                URI location = uriInfo.getAbsolutePathBuilder().path(response.getId().toString()).build();
                return Response.created(location).entity(response).build();
            });
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Actualizar los datos de un emisor")
    @APIResponse(responseCode = "200", description = "Actualizado")
    @APIResponse(responseCode = "404", description = "Emisor no encontrado")
    public Uni<Response> actualizar(
            @Parameter(description = "Id del emisor", required = true) @PathParam("id") UUID id,
            @Valid ActualizarEmisorRequest request) {
        return emisorService.actualizarEmisor(id, request).map(response -> Response.ok(response).build());
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Obtener un emisor por id")
    @APIResponse(responseCode = "200", description = "Encontrado")
    @APIResponse(responseCode = "404", description = "Emisor no encontrado")
    public Uni<Response> obtenerPorId(
            @Parameter(description = "Id del emisor", required = true) @PathParam("id") UUID id) {
        return emisorService.obtenerPorId(id).map(response -> Response.ok(response).build());
    }

    @GET
    @Operation(summary = "Listar emisores", description = "Paginado.")
    @APIResponse(responseCode = "200", description = "Listado")
    public Uni<Response> listar(@Valid @BeanParam PageRequest pageRequest) {
        return emisorService.listarPaginado(pageRequest.getPage(), pageRequest.getLimit())
            .map((PagedResponse<EmisorResponse> paged) -> Response.ok(paged).build());
    }

    @POST
    @Path("/{id}/certificado")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Subir el certificado .p12 del emisor",
        description = "El contenido y la contraseña se cifran en reposo antes de guardarse; ningún endpoint "
            + "de lectura los devuelve. Cualquier certificado ACTIVO previo del emisor queda REVOCADO.")
    @APIResponse(responseCode = "201", description = "Certificado cargado")
    @APIResponse(responseCode = "400", description = "El archivo no es un .p12 válido o la contraseña no lo abre")
    @APIResponse(responseCode = "404", description = "Emisor no encontrado")
    public Uni<Response> subirCertificado(
            @Parameter(description = "Id del emisor", required = true) @PathParam("id") UUID id,
            @Valid @BeanParam SubirCertificadoRequest form) {
        // Leer el archivo subido es IO bloqueante — se corre en el worker
        // pool, y se vuelve al contexto de Vert.x antes de que
        // certificadoService.subirCertificado (que abre una sesión de
        // Hibernate Reactive) siga la cadena. Mismo motivo que el salto de
        // contexto dentro de CertificadoService.
        io.vertx.core.Context callingContext = Vertx.currentContext();
        return Uni.createFrom().item(() -> leerArchivo(form))
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
            .emitOn(runnable -> callingContext.runOnContext(ignored -> runnable.run()))
            .onItem().transformToUni(contenido ->
                certificadoService.subirCertificado(id, contenido, form.contrasena))
            .map(response -> Response.status(Response.Status.CREATED).entity(response).build());
    }

    @GET
    @Path("/{id}/certificado")
    @Operation(summary = "Consultar la metadata del certificado activo del emisor",
        description = "Nunca incluye el contenido del .p12 ni su contraseña.")
    @APIResponse(responseCode = "200", description = "Encontrado")
    @APIResponse(responseCode = "404", description = "El emisor no tiene un certificado activo")
    public Uni<Response> obtenerCertificadoActivo(
            @Parameter(description = "Id del emisor", required = true) @PathParam("id") UUID id) {
        return certificadoService.obtenerActivoPorEmisor(id)
            .map((CertificadoMetadataResponse response) -> Response.ok(response).build());
    }

    @PUT
    @Path("/{id}/configuracion/{clave}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Establecer un ajuste de configuración del emisor",
        description = "Crea o actualiza (upsert) el valor de esa clave — pensado para ajustes opcionales que no ameritan una columna propia en Emisor: URLs del SRI a sobreescribir, plantilla del RIDE, política de reintentos, etc.")
    @APIResponse(responseCode = "200", description = "Guardado")
    @APIResponse(responseCode = "404", description = "Emisor no encontrado")
    public Uni<Response> establecerConfiguracion(
            @Parameter(description = "Id del emisor", required = true) @PathParam("id") UUID id,
            @Parameter(description = "Clave del ajuste", required = true) @PathParam("clave") String clave,
            @Valid EstablecerConfiguracionRequest request) {
        return configuracionService.establecer(id, clave, request.getValor())
            .map(response -> Response.ok(response).build());
    }

    @GET
    @Path("/{id}/configuracion")
    @Operation(summary = "Listar los ajustes de configuración de un emisor")
    @APIResponse(responseCode = "200", description = "Listado")
    public Uni<Response> listarConfiguracion(
            @Parameter(description = "Id del emisor", required = true) @PathParam("id") UUID id) {
        return configuracionService.listar(id)
            .map((java.util.List<ConfiguracionEmisorResponse> ajustes) -> Response.ok(ajustes).build());
    }

    @GET
    @Path("/{id}/configuracion/{clave}")
    @Operation(summary = "Obtener un ajuste de configuración por clave")
    @APIResponse(responseCode = "200", description = "Encontrado")
    @APIResponse(responseCode = "404", description = "No existe esa clave para el emisor")
    public Uni<Response> obtenerConfiguracion(
            @Parameter(description = "Id del emisor", required = true) @PathParam("id") UUID id,
            @Parameter(description = "Clave del ajuste", required = true) @PathParam("clave") String clave) {
        return configuracionService.obtener(id, clave).map(response -> Response.ok(response).build());
    }

    @DELETE
    @Path("/{id}/configuracion/{clave}")
    @Operation(summary = "Eliminar un ajuste de configuración")
    @APIResponse(responseCode = "204", description = "Eliminado")
    @APIResponse(responseCode = "404", description = "No existe esa clave para el emisor")
    public Uni<Response> eliminarConfiguracion(
            @Parameter(description = "Id del emisor", required = true) @PathParam("id") UUID id,
            @Parameter(description = "Clave del ajuste", required = true) @PathParam("clave") String clave) {
        return configuracionService.eliminar(id, clave).map(ignorado -> Response.noContent().build());
    }

    private byte[] leerArchivo(SubirCertificadoRequest form) {
        try {
            return Files.readAllBytes(form.archivo.uploadedFile());
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el archivo del certificado subido", e);
        }
    }
}
