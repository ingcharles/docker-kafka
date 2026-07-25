package com.facturacion.firma.shared.error.mapper;

import org.jboss.logging.Logger;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.facturacion.firma.shared.error.ApiErrorResponse;

/**
 * Manejador de respaldo (catch-all): las excepciones propias del framework
 * ({@link WebApplicationException}, p. ej. un 404 de ruta inexistente)
 * conservan su código de estado; cualquier otra excepción no mapeada se
 * convierte en un 500 genérico sin exponer el stack trace al cliente.
 */
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(GenericExceptionMapper.class);

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof WebApplicationException webEx) {
            return webEx.getResponse();
        }
        LOG.error("Excepción no manejada", exception);
        ApiErrorResponse body = new ApiErrorResponse(500, "Internal Server Error",
            "Ocurrió un error inesperado", null);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(body).build();
    }
}
