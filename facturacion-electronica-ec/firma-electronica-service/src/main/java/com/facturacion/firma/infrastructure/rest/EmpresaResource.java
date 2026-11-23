package com.facturacion.firma.infrastructure.rest;

import java.net.URI;
import java.nio.file.Files;
import java.util.List;
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

import com.facturacion.commons.exception.ErrorNegocioException;
import com.facturacion.commons.reactive.TrabajoBloqueante;
import com.facturacion.commons.rest.Respuestas;
import com.facturacion.commons.paging.PageRequest;
import com.facturacion.commons.paging.PagedResponse;
import com.facturacion.commons.security.PermiteSesion;
import com.facturacion.commons.security.TokenPrincipal;
import com.facturacion.firma.application.dto.ActualizarEmpresaRequest;
import com.facturacion.firma.application.dto.CertificadoMetadataResponse;
import com.facturacion.firma.application.dto.ConfiguracionEmpresaResponse;
import com.facturacion.firma.application.dto.CrearEmpresaRequest;
import com.facturacion.firma.application.dto.EmpresaResponse;
import com.facturacion.firma.application.dto.EstablecerConfiguracionRequest;
import com.facturacion.firma.application.dto.SubirCertificadoRequest;
import com.facturacion.firma.application.dto.SubirLogoRequest;
import com.facturacion.firma.application.service.LogoEmpresaService;
import com.facturacion.firma.application.service.CertificadoService;
import com.facturacion.firma.application.service.ConfiguracionEmpresaService;
import com.facturacion.firma.application.service.EmpresaService;
import com.facturacion.firma.infrastructure.security.AccesoEmpresa;
import com.facturacion.firma.shared.error.FirmaError;

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
    private final LogoEmpresaService logoService;

    @Inject
    public EmpresaResource(EmpresaService empresaService, CertificadoService certificadoService,
            ConfiguracionEmpresaService configuracionService, TokenPrincipal tokenPrincipal,
            AccesoEmpresa acceso, LogoEmpresaService logoService) {
        this.empresaService = empresaService;
        this.certificadoService = certificadoService;
        this.configuracionService = configuracionService;
        this.tokenPrincipal = tokenPrincipal;
        this.acceso = acceso;
        this.logoService = logoService;
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
        if (tokenPrincipal.getRuc() != null) {
            tokenPrincipal.exigirEmpresa(request.getRuc());
        }
        return empresaService.crearEmpresa(request, tokenPrincipal.getUsuarioId())
            .map(response -> {
                URI location = uriInfo.getAbsolutePathBuilder().path(response.getId().toString()).build();
                return Respuestas.creado(location, response);
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
            .map(response -> Respuestas.actualizado(response));
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
            @Valid @BeanParam SubirCertificadoRequest form,
            @Context UriInfo uriInfo) {
        // Leer el archivo subido es IO bloqueante: va por TrabajoBloqueante (fuera del event loop y de vuelta al contexto
        // de Vert.x antes de que certificadoService abra una sesión de Hibernate Reactive).
        // Primero se comprueba que la empresa sea la de la api-key: no se lee el archivo subido si no lo es.
        // OJO: solo la lectura del archivo va fuera del event loop; la comprobación de acceso usa Hibernate Reactive y
        // debe correr en el hilo de eventos de Vert.x (envolver toda la cadena fallaría con HR000068).
        return acceso.exigirEmpresaAdmin(id)
            .chain(() -> TrabajoBloqueante.ejecutar(() -> leerArchivo(form)))
            .onItem().transformToUni(contenido ->
                certificadoService.subirCertificado(id, contenido, form.contrasena))
            .map(response -> Respuestas.creado(uriInfo.getAbsolutePath(), response));
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
            .map(ignorado -> Respuestas.sinContenido());
    }

    @POST
    @Path("/{id}/logo")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Subir el logo de la empresa (PNG o JPEG, hasta 200 KB)",
        description = "Se guarda en la tabla empresa y aparece en el recuadro del emisor del RIDE de los comprobantes que se "
            + "autoricen a partir de ese momento. Reemplaza el logo anterior.")
    @APIResponse(responseCode = "201", description = "Logo guardado: devuelve el tipo detectado y el tamaño")
    @APIResponse(responseCode = "400", description = "No es un PNG/JPEG o pesa más de 200 KB")
    @APIResponse(responseCode = "404", description = "Empresa no encontrada")
    public Uni<Response> subirLogo(
            @Parameter(description = "Id de la empresa", required = true) @PathParam("id") UUID id,
            @Valid @BeanParam SubirLogoRequest form,
            @Context UriInfo uriInfo) {
        // Igual que el certificado: el archivo se lee fuera del event loop y se vuelve al contexto de Vert.x para Hibernate.
        return acceso.exigirEmpresaAdmin(id)
            .chain(() -> TrabajoBloqueante.ejecutar(() -> leerArchivoLogo(form)))
            .onItem().transformToUni(contenido -> logoService.subir(id, contenido))
            .map(logo -> Respuestas.creado(uriInfo.getAbsolutePath(), logo));
    }

    @GET
    @Path("/{id}/logo")
    @Produces({"image/png", "image/jpeg"})
    @Operation(summary = "Obtener el logo de la empresa")
    @APIResponse(responseCode = "200", description = "La imagen")
    @APIResponse(responseCode = "404", description = "La empresa no tiene logo")
    public Uni<Response> obtenerLogo(
            @Parameter(description = "Id de la empresa", required = true) @PathParam("id") UUID id) {
        return acceso.exigirEmpresa(id).chain(() -> logoService.obtener(id))
            .map(logo -> Response.ok(logo.contenido()).type(logo.tipo()).build());
    }

    @DELETE
    @Path("/{id}/logo")
    @Operation(summary = "Quitar el logo de la empresa", description = "Los RIDE siguientes salen sin logo.")
    @APIResponse(responseCode = "204", description = "Logo quitado")
    @APIResponse(responseCode = "404", description = "La empresa no tiene logo")
    public Uni<Response> eliminarLogo(
            @Parameter(description = "Id de la empresa", required = true) @PathParam("id") UUID id) {
        return acceso.exigirEmpresaAdmin(id).chain(() -> logoService.eliminar(id))
            .map(ignorado -> Respuestas.sinContenido());
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
            .map(response -> Respuestas.actualizado(response));
    }

    @GET
    @Path("/{id}/configuracion")
    @Operation(summary = "Listar los ajustes de configuración de una empresa")
    @APIResponse(responseCode = "200", description = "Listado")
    public Uni<Response> listarConfiguracion(
            @Parameter(description = "Id de la empresa", required = true) @PathParam("id") UUID id) {
        return acceso.exigirEmpresa(id).chain(() -> configuracionService.listar(id))
            .map((List<ConfiguracionEmpresaResponse> ajustes) -> Response.ok(ajustes).build());
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
            .map(ignorado -> Respuestas.sinContenido());
    }

    private byte[] leerArchivoLogo(SubirLogoRequest form) {
        try {
            return Files.readAllBytes(form.archivo.uploadedFile());
        } catch (Exception e) {
            throw new ErrorNegocioException(FirmaError.ARCHIVO_LOGO_ILEGIBLE);
        }
    }

    private byte[] leerArchivo(SubirCertificadoRequest form) {
        try {
            return Files.readAllBytes(form.archivo.uploadedFile());
        } catch (Exception e) {
            throw new ErrorNegocioException(FirmaError.ARCHIVO_CERTIFICADO_ILEGIBLE);
        }
    }
}
