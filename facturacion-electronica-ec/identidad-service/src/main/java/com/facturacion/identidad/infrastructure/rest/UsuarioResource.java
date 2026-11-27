package com.facturacion.identidad.infrastructure.rest;

import java.net.URI;
import java.util.List;

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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.smallrye.mutiny.Uni;

import com.facturacion.commons.rest.Respuestas;
import com.facturacion.identidad.application.dto.ActualizarPerfilRequest;
import com.facturacion.identidad.application.dto.CambiarPasswordRequest;
import com.facturacion.identidad.application.dto.CrearUsuarioRequest;
import com.facturacion.identidad.application.dto.EmpresaRolResponse;
import com.facturacion.identidad.application.dto.LoginRequest;
import com.facturacion.identidad.application.dto.TokenResponse;
import com.facturacion.identidad.application.dto.UsuarioResponse;
import com.facturacion.identidad.application.service.LimiteDeLogin;
import com.facturacion.identidad.application.service.UsuarioService;

@Path("/usuarios")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Usuarios", description = "Cuentas de usuario, login, y asociación a empresas (ruc)")
public class UsuarioResource {

    private final UsuarioService usuarioService;
    private final SesionActual sesionActual;
    private final LimiteDeLogin limiteDeLogin;

    @Inject
    public UsuarioResource(UsuarioService usuarioService, SesionActual sesionActual, LimiteDeLogin limiteDeLogin) {
        this.usuarioService = usuarioService;
        this.sesionActual = sesionActual;
        this.limiteDeLogin = limiteDeLogin;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Crear una cuenta de usuario")
    @APIResponse(responseCode = "201", description = "Usuario creado")
    @APIResponse(responseCode = "409", description = "Ya existe un usuario con ese email")
    public Uni<Response> crear(@Valid CrearUsuarioRequest request, @Context UriInfo uriInfo) {
        return usuarioService.crear(request).map(response -> {
            URI location = uriInfo.getAbsolutePathBuilder().path(response.getId().toString()).build();
            return Respuestas.creado(location, response);
        });
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Autenticarse", description = "Devuelve un token de sesión — solo sirve para hablar con este servicio, no es una api-key.")
    @APIResponse(responseCode = "200", description = "Token de sesión")
    @APIResponse(responseCode = "401", description = "Email o contraseña incorrectos")
    @APIResponse(responseCode = "429", description = "Demasiados intentos sin éxito en la cuenta; ver Retry-After")
    public Uni<Response> iniciarSesion(@Valid LoginRequest request) {
        return limiteDeLogin.proteger(request.getEmail(), () -> usuarioService.iniciarSesion(request))
            .map(Respuestas::actualizado);
    }

    @GET
    @Path("/me")
    @Autenticado
    @SecurityRequirement(name = "token")
    @Operation(summary = "Datos del usuario autenticado")
    public Uni<UsuarioResponse> obtenerPerfil() {
        return usuarioService.obtenerPorId(sesionActual.getUsuarioId());
    }

    @PATCH
    @Path("/me")
    @Autenticado
    @SecurityRequirement(name = "token")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Actualizar el perfil del usuario autenticado", description = "Solo nombres/apellidos — el email de login no se cambia por aquí. PATCH porque solo toca una parte del recurso Usuario, no lo reemplaza entero.")
    public Uni<Response> actualizarPerfil(@Valid ActualizarPerfilRequest request) {
        return usuarioService.actualizarPerfil(sesionActual.getUsuarioId(), request).map(Respuestas::actualizado);
    }

    @PATCH
    @Path("/me/password")
    @Autenticado
    @SecurityRequirement(name = "token")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Cambiar la contraseña del usuario autenticado", description = "Exige la contraseña actual.")
    @APIResponse(responseCode = "204", description = "Contraseña cambiada")
    @APIResponse(responseCode = "401", description = "La contraseña actual no es correcta")
    public Uni<Response> cambiarPassword(@Valid CambiarPasswordRequest request) {
        return usuarioService.cambiarPassword(sesionActual.getUsuarioId(), request)
            .map(ignorado -> Respuestas.sinContenido());
    }

    @DELETE
    @Path("/me/empresas/{ruc}")
    @Autenticado
    @SecurityRequirement(name = "token")
    @Operation(summary = "Abandonar una empresa (ruc)", description = "No se permite si eres el único ADMIN que le queda.")
    @APIResponse(responseCode = "204", description = "Ya no estás asociado a ese ruc")
    @APIResponse(responseCode = "404", description = "No estabas asociado a ese ruc")
    @APIResponse(responseCode = "409", description = "Dejarías al ruc sin ningún ADMIN")
    public Uni<Response> salirDeEmpresa(
            @Parameter(description = "RUC de la empresa", required = true) @PathParam("ruc") String ruc) {
        return usuarioService.salirDeEmpresa(sesionActual.getUsuarioId(), ruc)
            .map(ignorado -> Respuestas.sinContenido());
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
