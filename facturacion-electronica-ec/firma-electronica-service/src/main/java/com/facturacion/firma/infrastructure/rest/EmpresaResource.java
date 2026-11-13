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
import jakarta.ws.rs.PATCH;
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

import com.facturacion.firma.application.dto.ActualizarEmpresaRequest;
import com.facturacion.firma.application.dto.CertificadoMetadataResponse;
import com.facturacion.firma.application.dto.ConfiguracionEmpresaResponse;
import com.facturacion.firma.application.dto.CrearEmpresaRequest;
import com.facturacion.firma.application.dto.EmpresaResponse;
import com.facturacion.firma.application.dto.EstablecerConfiguracionRequest;
import com.facturacion.firma.application.dto.SubirCertificadoRequest;
import com.facturacion.firma.application.service.CertificadoService;
import com.facturacion.firma.application.service.ConfiguracionEmpresaService;
import com.facturacion.firma.application.service.EmpresaService;
import com.facturacion.commons.security.PermiteSesion;
import com.facturacion.firma.infrastructure.security.AccesoEmpresa;
import com.facturacion.commons.security.TokenPrincipal;
import com.facturacion.commons.exception.SinPermisoException;
import com.facturacion.commons.paging.PageRequest;
import com.facturacion.commons.paging.PagedResponse;

/**
 * CRUD de empresas (multi-tenant) y carga del certificado de firma
 * electrónica. Es la única excepción a "cero REST entre servicios" del
 * stack: {@code comprobantes-service} y {@code sri-conector-service}
 * mantienen su propia copia de los datos de empresa vía los eventos
 * publicados en {@code facturacion.empresa.eventos.v1} (ver ARQUITECTURA.md)
 * — este REST es solo para el cliente/administrador externo.
 */
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Empresas", description = "Contribuyentes que emiten comprobantes electrónicos, y su certificado de firma")
public class EmpresaResource {

    private final EmpresaService empresaService;
    private final CertificadoService certificadoService;
    private final ConfiguracionEmpresaService configuracionService;
    private final TokenPrincipal tokenPrincipal;
    private final AccesoEmpresa acceso;

    @Inject
    public EmpresaResource(EmpresaService empresaService, CertificadoService certificadoService,
            ConfiguracionEmpresaService configuracionService, TokenPrincipal tokenPrincipal,
            AccesoEmpresa acceso) {
        this.empresaService = empresaService;
        this.certificadoService = certificadoService;
        this.configuracionService = configuracionService;
        this.tokenPrincipal = tokenPrincipal;
        this.acceso = acceso;
    }

    /**
     * Único endpoint del stack que acepta, además de una api-key, un token de
     * sesión de identidad-service ({@link PermiteSesion}) — es la única forma
     * de dar de alta una empresa genuinamente nueva: una api-key no se puede
     * emitir para un ruc al que el usuario no esté ya asociado, y esa
     * asociación exige que el ruc ya corresponda a una empresa real, así que
     * para una empresa que todavía no existe ese camino nunca se cumple. Con
     * un token de sesión no hay ruc que comparar todavía (no hace falta:
     * no hay ningún dueño previo que proteger), así que el chequeo de
     * titularidad se salta — pero el usuario que la creó queda registrado
     * como {@code ADMIN} de esa empresa automáticamente (ver
     * {@code EmpresaService.crearEmpresa}, evento {@code EmpresaRegistrada}
     * con {@code usuarioCreadorId}, consumido por identidad-service).
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @PermiteSesion
    @Operation(summary = "Registrar una empresa", description = "Crea el contribuyente que emitirá comprobantes; el certificado se sube después con POST /{id}/certificado. Acepta un token de sesión de identidad-service o una api-key — ver PermiteSesion.")
    @APIResponse(responseCode = "201", description = "Empresa creada — el usuario autenticado queda como ADMIN de ella")
    @APIResponse(responseCode = "400", description = "Solicitud inválida")
    @APIResponse(responseCode = "403", description = "La api-key no es del ruc de la empresa")
    @APIResponse(responseCode = "409", description = "Ya existe una empresa con ese RUC")
    public Uni<Response> crear(@Valid CrearEmpresaRequest request, @Context UriInfo uriInfo) {
        // tokenPrincipal.getRuc() == null significa token de sesión (sin
        // claim ruc) — nada que comparar todavía, ver el javadoc de arriba.
        if (tokenPrincipal.getRuc() != null && !tokenPrincipal.esDeLaEmpresa(request.getRuc())) {
            throw new SinPermisoException(
                "La api-key usada es para el ruc " + tokenPrincipal.getRuc() + ", no para " + request.getRuc());
        }
        return empresaService.crearEmpresa(request, tokenPrincipal.getUsuarioId())
            .map(response -> {
                URI location = uriInfo.getAbsolutePathBuilder().path(response.getId().toString()).build();
                return Response.created(location).entity(response).build();
            });
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Actualizar los datos de una empresa")
    @APIResponse(responseCode = "200", description = "Actualizado")
    @APIResponse(responseCode = "404", description = "Empresa no encontrada")
    public Uni<Response> actualizar(
            @Parameter(description = "Id de la empresa", required = true) @PathParam("id") UUID id,
            @Valid ActualizarEmpresaRequest request) {
        return acceso.exigirEmpresaAdmin(id).chain(() -> empresaService.actualizarEmpresa(id, request))
            .map(response -> Response.ok(response).build());
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Obtener una empresa por id")
    @APIResponse(responseCode = "200", description = "Encontrado")
    @APIResponse(responseCode = "404", description = "Empresa no encontrada")
    public Uni<Response> obtenerPorId(
            @Parameter(description = "Id de la empresa", required = true) @PathParam("id") UUID id) {
        return acceso.exigirEmpresa(id).chain(() -> empresaService.obtenerPorId(id))
            .map(response -> Response.ok(response).build());
    }

    @GET
    @Operation(summary = "Listar empresas", description = "Paginado.")
    @APIResponse(responseCode = "200", description = "Listado")
    public Uni<Response> listar(@Valid @BeanParam PageRequest pageRequest) {
        // Solo la empresa de la api-key: este listado no cruza empresas.
        return empresaService.listarDeLaEmpresa(tokenPrincipal.getRuc(), pageRequest.getPage(), pageRequest.getLimit())
            .map((PagedResponse<EmpresaResponse> paged) -> Response.ok(paged).build());
    }

    @POST
    @Path("/{id}/certificado")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Subir el certificado .p12 de la empresa",
        description = "El contenido y la contraseña se cifran en reposo antes de guardarse; ningún endpoint "
            + "de lectura los devuelve. Cualquier certificado ACTIVO previo de la empresa queda REVOCADO.")
    @APIResponse(responseCode = "201", description = "Certificado cargado")
    @APIResponse(responseCode = "400", description = "El archivo no es un .p12 válido o la contraseña no lo abre")
    @APIResponse(responseCode = "404", description = "Empresa no encontrada")
    public Uni<Response> subirCertificado(
            @Parameter(description = "Id de la empresa", required = true) @PathParam("id") UUID id,
            @Valid @BeanParam SubirCertificadoRequest form) {
        // Leer el archivo subido es IO bloqueante — se corre en el worker
        // pool, y se vuelve al contexto de Vert.x antes de que
        // certificadoService.subirCertificado (que abre una sesión de
        // Hibernate Reactive) siga la cadena. Mismo motivo que el salto de
        // contexto dentro de CertificadoService.
        io.vertx.core.Context callingContext = Vertx.currentContext();
        // Primero se comprueba que la empresa sea la de la api-key: no se lee el archivo subido si no lo es.
        // OJO: el worker pool es SOLO para leer el archivo (IO bloqueante). La comprobación de acceso usa Hibernate
        // Reactive y debe correr en el hilo de eventos de Vert.x; si runSubscriptionOn envolviera toda la cadena, fallaría
        // con HR000068.
        return acceso.exigirEmpresaAdmin(id)
            .chain(() -> Uni.createFrom().item(() -> leerArchivo(form))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .emitOn(runnable -> callingContext.runOnContext(ignored -> runnable.run())))
            .onItem().transformToUni(contenido ->
                certificadoService.subirCertificado(id, contenido, form.contrasena))
            .map(response -> Response.status(Response.Status.CREATED).entity(response).build());
    }

    @GET
    @Path("/{id}/certificado")
    @Operation(summary = "Consultar la metadata del certificado activo de la empresa",
        description = "Nunca incluye el contenido del .p12 ni su contraseña.")
    @APIResponse(responseCode = "200", description = "Encontrado")
    @APIResponse(responseCode = "404", description = "La empresa no tiene un certificado activo")
    public Uni<Response> obtenerCertificadoActivo(
            @Parameter(description = "Id de la empresa", required = true) @PathParam("id") UUID id) {
        return acceso.exigirEmpresa(id).chain(() -> certificadoService.obtenerActivoPorEmpresa(id))
            .map((CertificadoMetadataResponse response) -> Response.ok(response).build());
    }

    @DELETE
    @Path("/{id}/certificado")
    @Operation(summary = "Revocar el certificado activo de la empresa",
        description = "Sin subir uno nuevo en su lugar (ej. certificado comprometido) — a diferencia de POST /{id}/certificado, que revoca el anterior implícitamente al reemplazarlo.")
    @APIResponse(responseCode = "204", description = "Revocado")
    @APIResponse(responseCode = "404", description = "La empresa no tiene un certificado activo")
    public Uni<Response> revocarCertificado(
            @Parameter(description = "Id de la empresa", required = true) @PathParam("id") UUID id) {
        return acceso.exigirEmpresaAdmin(id).chain(() -> certificadoService.revocarActivo(id))
            .map(ignorado -> Response.noContent().build());
    }

    @POST
    @Path("/{id}/configuracion/{clave}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Crear un ajuste de configuración de la empresa",
        description = "Registra la clave por primera vez — pensado para ajustes opcionales que no ameritan una columna propia en Empresa: URLs del SRI a sobreescribir, plantilla del RIDE, política de reintentos, etc.")
    @APIResponse(responseCode = "200", description = "Creado")
    @APIResponse(responseCode = "404", description = "Empresa no encontrada")
    @APIResponse(responseCode = "409", description = "Ya existe esa clave para esta empresa — usa PATCH para cambiar su valor")
    public Uni<Response> crearConfiguracion(
            @Parameter(description = "Id de la empresa", required = true) @PathParam("id") UUID id,
            @Parameter(description = "Clave del ajuste", required = true) @PathParam("clave") String clave,
            @Valid EstablecerConfiguracionRequest request) {
        return acceso.exigirEmpresaAdmin(id).chain(() -> configuracionService.crear(id, clave, request.getValor()))
            .map(response -> Response.ok(response).build());
    }

    @PATCH
    @Path("/{id}/configuracion/{clave}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Cambiar el valor de un ajuste de configuración ya existente",
        description = "PATCH porque modifica un ajuste puntual dentro de la configuración de la empresa, no reemplaza la empresa entera.")
    @APIResponse(responseCode = "200", description = "Actualizado")
    @APIResponse(responseCode = "404", description = "Empresa no encontrada, o esa clave todavía no existe — usa POST para crearla")
    public Uni<Response> actualizarConfiguracion(
            @Parameter(description = "Id de la empresa", required = true) @PathParam("id") UUID id,
            @Parameter(description = "Clave del ajuste", required = true) @PathParam("clave") String clave,
            @Valid EstablecerConfiguracionRequest request) {
        return acceso.exigirEmpresaAdmin(id).chain(() -> configuracionService.actualizar(id, clave, request.getValor()))
            .map(response -> Response.ok(response).build());
    }

    @GET
    @Path("/{id}/configuracion")
    @Operation(summary = "Listar los ajustes de configuración de una empresa")
    @APIResponse(responseCode = "200", description = "Listado")
    public Uni<Response> listarConfiguracion(
            @Parameter(description = "Id de la empresa", required = true) @PathParam("id") UUID id) {
        return acceso.exigirEmpresa(id).chain(() -> configuracionService.listar(id))
            .map((java.util.List<ConfiguracionEmpresaResponse> ajustes) -> Response.ok(ajustes).build());
    }

    @GET
    @Path("/{id}/configuracion/{clave}")
    @Operation(summary = "Obtener un ajuste de configuración por clave")
    @APIResponse(responseCode = "200", description = "Encontrado")
    @APIResponse(responseCode = "404", description = "No existe esa clave para la empresa")
    public Uni<Response> obtenerConfiguracion(
            @Parameter(description = "Id de la empresa", required = true) @PathParam("id") UUID id,
            @Parameter(description = "Clave del ajuste", required = true) @PathParam("clave") String clave) {
        return acceso.exigirEmpresa(id).chain(() -> configuracionService.obtener(id, clave))
            .map(response -> Response.ok(response).build());
    }

    @DELETE
    @Path("/{id}/configuracion/{clave}")
    @Operation(summary = "Eliminar un ajuste de configuración")
    @APIResponse(responseCode = "204", description = "Eliminado")
    @APIResponse(responseCode = "404", description = "No existe esa clave para la empresa")
    public Uni<Response> eliminarConfiguracion(
            @Parameter(description = "Id de la empresa", required = true) @PathParam("id") UUID id,
            @Parameter(description = "Clave del ajuste", required = true) @PathParam("clave") String clave) {
        return acceso.exigirEmpresaAdmin(id).chain(() -> configuracionService.eliminar(id, clave))
            .map(ignorado -> Response.noContent().build());
    }

    private byte[] leerArchivo(SubirCertificadoRequest form) {
        try {
            return Files.readAllBytes(form.archivo.uploadedFile());
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el archivo del certificado subido", e);
        }
    }
}
