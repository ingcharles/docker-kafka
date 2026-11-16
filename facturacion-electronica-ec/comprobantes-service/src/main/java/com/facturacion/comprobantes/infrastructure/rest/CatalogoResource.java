package com.facturacion.comprobantes.infrastructure.rest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.smallrye.mutiny.Uni;

import com.facturacion.commons.rest.Respuestas;
import com.facturacion.commons.exception.ErrorNegocioException;
import com.facturacion.comprobantes.application.service.CatalogoAdminService;
import com.facturacion.comprobantes.domain.catalogo.CatalogoDef;
import com.facturacion.comprobantes.domain.model.enums.Catalogo;
import com.facturacion.comprobantes.shared.constant.ApiConstants;
import com.facturacion.comprobantes.shared.error.CodigoError;

/**
 * Catálogos del SRI de este servicio. Lectura: cualquier api-key válida (filtro global). Escritura: además exige el header
 * {@value ApiConstants#HEADER_ADMIN_TOKEN} — los catálogos son globales, una api-key de empresa no debe poder
 * cambiarlos para todas las demás.
 */
@Path("/catalogos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CatalogoResource {

    private final CatalogoAdminService service;
    private final Optional<String> tokenAdmin;

    @Inject
    public CatalogoResource(CatalogoAdminService service,
            @ConfigProperty(name = "catalogos.admin.token") Optional<String> tokenAdmin) {
        this.service = service;
        this.tokenAdmin = tokenAdmin.filter(t -> !t.isBlank());
    }

    @GET
    public List<String> catalogos() {
        return Catalogo.nombres();
    }

    /** {@code vigentes=true}: solo activos y vigentes en {@code fecha} (yyyy-MM-dd, hoy por defecto). */
    @GET
    @Path("/{catalogo}")
    public Uni<List<Map<String, Object>>> listar(@PathParam("catalogo") String catalogo,
            @QueryParam("vigentes") boolean vigentes, @QueryParam("fecha") String fecha) {
        CatalogoDef def = definicion(catalogo);
        LocalDate dia;
        try {
            dia = fecha == null || fecha.isBlank() ? null : LocalDate.parse(fecha);
        } catch (DateTimeParseException e) {
            throw new ErrorNegocioException(CodigoError.FECHA_INVALIDA, fecha);
        }
        return service.listar(def, vigentes, dia);
    }

    /** Crea o actualiza una fila por su clave. Dar de baja = mandar {@code "activo": false}. */
    @PUT
    @Path("/{catalogo}")
    public Uni<Response> guardar(@HeaderParam(ApiConstants.HEADER_ADMIN_TOKEN) String token,
            @PathParam("catalogo") String catalogo, Map<String, Object> body) {
        exigirAdmin(token);
        return service.guardar(definicion(catalogo), body).map(Respuestas::actualizado);
    }

    private static CatalogoDef definicion(String catalogo) {
        return CatalogoDef.buscar(catalogo).orElseThrow(
            () -> new ErrorNegocioException(CodigoError.CATALOGO_NO_ENCONTRADO, catalogo, Catalogo.nombres()));
    }

    private void exigirAdmin(String token) {
        if (tokenAdmin.isEmpty()) {
            throw new ErrorNegocioException(CodigoError.ESCRITURA_DESHABILITADA);
        }
        boolean coincide = token != null && MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
            tokenAdmin.get().getBytes(StandardCharsets.UTF_8));
        if (!coincide) {
            throw new ErrorNegocioException(CodigoError.TOKEN_ADMIN_INVALIDO, ApiConstants.HEADER_ADMIN_TOKEN);
        }
    }
}
