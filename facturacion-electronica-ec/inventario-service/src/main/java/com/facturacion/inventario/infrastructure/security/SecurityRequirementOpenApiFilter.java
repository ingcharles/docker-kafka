package com.facturacion.inventario.infrastructure.security;

import java.util.List;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;

/**
 * {@code ApiKeyAuthFilter} es un {@code ContainerRequestFilter} global sin
 * ninguna anotación que SmallRye OpenAPI reconozca automáticamente (no
 * {@code @RolesAllowed}, no {@code @Authenticated} de Quarkus Security) — sin
 * este filtro, ningún endpoint queda marcado con {@code security} en el
 * documento OpenAPI generado, y el botón Authorize de Swagger UI nunca
 * adjunta el token a ninguna llamada (bug real, no cosmético — encontrado
 * primero en identidad-service para sus endpoints con {@code @Autenticado},
 * confirmado aquí para el resto del servicio). En vez de anotar cada
 * Resource uno por uno, este filtro aplica el requisito de forma global a
 * cualquier operación que todavía no declare su propio {@code security}
 * (registrado vía {@code quarkus.smallrye-openapi.filter}).
 */
public class SecurityRequirementOpenApiFilter implements OASFilter {

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        if (openAPI.getPaths() == null) {
            return;
        }
        SecurityRequirement requisito = OASFactory.createSecurityRequirement().addScheme("api-key");
        for (PathItem pathItem : openAPI.getPaths().getPathItems().values()) {
            aplicarSiFalta(pathItem.getGET(), requisito);
            aplicarSiFalta(pathItem.getPUT(), requisito);
            aplicarSiFalta(pathItem.getPOST(), requisito);
            aplicarSiFalta(pathItem.getDELETE(), requisito);
            aplicarSiFalta(pathItem.getOPTIONS(), requisito);
            aplicarSiFalta(pathItem.getHEAD(), requisito);
            aplicarSiFalta(pathItem.getPATCH(), requisito);
            aplicarSiFalta(pathItem.getTRACE(), requisito);
        }
    }

    private void aplicarSiFalta(Operation operation, SecurityRequirement requisito) {
        if (operation != null && operation.getSecurity() == null) {
            operation.setSecurity(List.of(requisito));
        }
    }
}
