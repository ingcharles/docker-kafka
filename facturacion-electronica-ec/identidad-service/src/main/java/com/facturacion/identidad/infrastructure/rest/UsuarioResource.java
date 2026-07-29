package com.facturacion.identidad.infrastructure.rest;

import java.net.URI;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.smallrye.mutiny.Uni;

import com.facturacion.identidad.application.dto.AsociarEmpresaRequest;
import com.facturacion.identidad.application.dto.CrearUsuarioRequest;
import com.facturacion.identidad.application.dto.EmpresaRolResponse;
import com.facturacion.identidad.application.dto.LoginRequest;
import com.facturacion.identidad.application.dto.TokenResponse;
import com.facturacion.identidad.application.dto.UsuarioResponse;
import com.facturacion.identidad.application.service.UsuarioService;

@Path("/usuarios")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Usuarios", description = "Cuentas de usuario, login, y asociación a empresas (ruc)")
public class UsuarioResource {

    private final UsuarioService usuarioService;
    private final SesionActual sesionActual;

    @Inject
    public UsuarioResource(UsuarioService usuarioService, SesionActual sesionActual) {
        this.usuarioService = usuarioService;
        this.sesionActual = sesionActual;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Crear una cuenta de usuario")
    @APIResponse(responseCode = "201", description = "Usuario creado")
    @APIResponse(responseCode = "409", description = "Ya existe un usuario con ese email")
    public Uni<Response> crear(@Valid CrearUsuarioRequest request, @Context UriInfo uriInfo) {
        return usuarioService.crear(request).map(response -> {
            URI location = uriInfo.getAbsolutePathBuilder().path(response.getId().toString()).build();
            return Response.created(location).entity(response).build();
        });
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Autenticarse", description = "Devuelve un token de sesión — solo sirve para hablar con este servicio, no es una api-key.")
    @APIResponse(responseCode = "200", description = "Token de sesión")
    @APIResponse(responseCode = "401", description = "Email o contraseña incorrectos")
    public Uni<TokenResponse> login(@Valid LoginRequest request) {
        return usuarioService.login(request);
    }

    @GET
    @Path("/me")
    @Autenticado
    @SecurityRequirement(name = "token")
    @Operation(summary = "Datos del usuario autenticado")
    public Uni<UsuarioResponse> obtenerMe() {
        return usuarioService.obtenerPorId(sesionActual.getUsuarioId());
    }

    @POST
    @Path("/me/empresas")
    @Autenticado
    @SecurityRequirement(name = "token")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Asociarse a una empresa (ruc)",
        description = "Sin flujo de invitación/aprobación en esta versión — ver ARQUITECTURA.md.")
    public Uni<EmpresaRolResponse> asociarEmpresa(@Valid AsociarEmpresaRequest request) {
        return usuarioService.asociarEmpresa(sesionActual.getUsuarioId(), request);
    }

    @GET
    @Path("/me/empresas")
    @Autenticado
    @SecurityRequirement(name = "token")
    @Operation(summary = "Listar las empresas (ruc) a las que está asociado el usuario autenticado")
    public Uni<List<EmpresaRolResponse>> listarEmpresas() {
        return usuarioService.listarEmpresas(sesionActual.getUsuarioId());
    }
}
