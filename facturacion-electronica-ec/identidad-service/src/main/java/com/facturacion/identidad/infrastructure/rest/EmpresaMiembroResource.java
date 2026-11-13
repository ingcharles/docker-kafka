package com.facturacion.identidad.infrastructure.rest;

import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.smallrye.mutiny.Uni;

import com.facturacion.identidad.application.dto.AgregarMiembroRequest;
import com.facturacion.identidad.application.dto.CambiarRolMiembroRequest;
import com.facturacion.identidad.application.dto.EmpresaRolResponse;
import com.facturacion.identidad.application.dto.MiembroEmpresaResponse;
import com.facturacion.identidad.application.service.UsuarioService;

/**
 * Gestión, por un ADMIN existente, de qué otros usuarios tienen acceso a su
 * empresa (ruc) — cierra el hueco de que {@code POST /usuarios/me/empresas}
 * permita a cualquiera autoasociarse sin que nadie más pueda verlo o
 * revocarlo. No es un flujo de invitación/aprobación (no hay estado
 * pendiente ni notificación al invitado): el ADMIN actúa directamente sobre
 * asociaciones que el propio usuario ya creó con {@code POST
 * /usuarios/me/empresas} — ver ARQUITECTURA.md.
 */
@Path("/empresas/{ruc}/usuarios")
@Produces(MediaType.APPLICATION_JSON)
@Autenticado
@SecurityRequirement(name = "token")
@Tag(name = "Miembros de empresa", description = "Qué usuarios tienen acceso a un ruc, y con qué rol — gestionado por un ADMIN de ese ruc")
public class EmpresaMiembroResource {

    private final UsuarioService usuarioService;
    private final SesionActual sesionActual;

    @Inject
    public EmpresaMiembroResource(UsuarioService usuarioService, SesionActual sesionActual) {
        this.usuarioService = usuarioService;
        this.sesionActual = sesionActual;
    }

    @GET
    @Operation(summary = "Listar los usuarios con acceso a este ruc", description = "Solo un ADMIN del ruc puede verlo.")
    @APIResponse(responseCode = "200", description = "Listado")
    @APIResponse(responseCode = "403", description = "El usuario autenticado no es ADMIN de este ruc")
    public Uni<List<MiembroEmpresaResponse>> listar(
            @Parameter(description = "RUC de la empresa", required = true) @PathParam("ruc") String ruc) {
        return usuarioService.listarMiembros(sesionActual.getUsuarioId(), ruc);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Dar acceso a un usuario a este ruc",
        description = "Solo un ADMIN del ruc puede hacerlo. El usuario debe estar ya registrado (se identifica por su correo). "
            + "Es la forma de sumar gente a una empresa que ya tiene dueño.")
    @APIResponse(responseCode = "201", description = "Acceso otorgado")
    @APIResponse(responseCode = "403", description = "El usuario autenticado no es ADMIN de este ruc")
    @APIResponse(responseCode = "404", description = "No hay ningún usuario registrado con ese correo")
    @APIResponse(responseCode = "409", description = "Ese usuario ya tiene acceso a este ruc")
    public Uni<Response> agregar(
            @Parameter(description = "RUC de la empresa", required = true) @PathParam("ruc") String ruc,
            @Valid AgregarMiembroRequest request) {
        return usuarioService.agregarMiembro(sesionActual.getUsuarioId(), ruc, request.getEmail(), request.getRol())
            .map(asociacion -> Response.status(Response.Status.CREATED).entity(asociacion).build());
    }

    @PATCH
    @Path("/{usuarioId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Cambiar el rol de un miembro", description = "Solo un ADMIN del ruc puede hacerlo; no se permite dejar el ruc sin ningún ADMIN. PATCH porque solo toca el campo rol, no reemplaza la asociación completa.")
    @APIResponse(responseCode = "200", description = "Rol actualizado")
    @APIResponse(responseCode = "403", description = "El usuario autenticado no es ADMIN de este ruc")
    @APIResponse(responseCode = "404", description = "Ese usuario no está asociado a este ruc")
    @APIResponse(responseCode = "409", description = "Dejaría al ruc sin ningún ADMIN")
    public Uni<EmpresaRolResponse> cambiarRol(
            @Parameter(description = "RUC de la empresa", required = true) @PathParam("ruc") String ruc,
            @Parameter(description = "Id del usuario miembro", required = true) @PathParam("usuarioId") UUID usuarioId,
            @Valid CambiarRolMiembroRequest request) {
        return usuarioService.cambiarRolMiembro(sesionActual.getUsuarioId(), ruc, usuarioId, request.getRol());
    }

    @DELETE
    @Path("/{usuarioId}")
    @Operation(summary = "Revocar el acceso de un miembro a este ruc", description = "Solo un ADMIN del ruc puede hacerlo; no se permite dejar el ruc sin ningún ADMIN.")
    @APIResponse(responseCode = "204", description = "Revocado")
    @APIResponse(responseCode = "403", description = "El usuario autenticado no es ADMIN de este ruc")
    @APIResponse(responseCode = "404", description = "Ese usuario no está asociado a este ruc")
    @APIResponse(responseCode = "409", description = "Dejaría al ruc sin ningún ADMIN")
    public Uni<Response> revocar(
            @Parameter(description = "RUC de la empresa", required = true) @PathParam("ruc") String ruc,
            @Parameter(description = "Id del usuario miembro", required = true) @PathParam("usuarioId") UUID usuarioId) {
        return usuarioService.revocarMiembro(sesionActual.getUsuarioId(), ruc, usuarioId)
            .map(ignorado -> Response.noContent().build());
    }
}
