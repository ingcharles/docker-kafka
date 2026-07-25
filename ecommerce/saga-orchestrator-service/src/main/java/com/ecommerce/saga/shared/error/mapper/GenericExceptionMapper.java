package com.ecommerce.saga.shared.error.mapper;

import org.jboss.logging.Logger;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.ecommerce.saga.shared.error.ApiErrorResponse;

/**
 * Manejador de respaldo (catch-all) para que ninguna excepción inesperada
 * termine mostrándole al cliente un stack trace completo. Al registrar
 * este mapper para {@code ExceptionMapper<Exception>}, JAX-RS lo usa como
 * último recurso para cualquier excepción que no tenga un mapper más
 * específico registrado — incluyendo las excepciones propias del framework
 * ({@link WebApplicationException}, por ejemplo un 404 cuando no hay ninguna
 * ruta que coincida, o un 405 cuando el verbo HTTP usado no es el correcto).
 * Esas excepciones del framework deben conservar su propio código de estado
 * HTTP, así que se detectan primero y se devuelven tal cual, en vez de
 * aplanarlas todas a un 500 genérico.
 */
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(GenericExceptionMapper.class);

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof WebApplicationException webEx) {
            return webEx.getResponse();
        }
        LOG.error("Unhandled exception", exception);
        ApiErrorResponse body = new ApiErrorResponse(500, "Internal Server Error",
            "An unexpected error occurred", null);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(body).build();
    }
}
