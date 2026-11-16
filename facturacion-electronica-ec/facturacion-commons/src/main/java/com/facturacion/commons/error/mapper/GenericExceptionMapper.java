package com.facturacion.commons.error.mapper;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

import com.facturacion.commons.error.ApiErrorResponse;
import com.facturacion.commons.error.ErrorComun;
import com.facturacion.commons.error.Mensajes;

/**
 * Manejador de respaldo (catch-all): las excepciones propias del framework ({@link WebApplicationException}, p. ej.
 * un 404 de ruta inexistente) conservan su código de estado; cualquier otra excepción no mapeada se convierte en un
 * 500 genérico sin exponer el stack trace al cliente.
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
            Mensajes.obtenerTexto(ErrorComun.ERROR_INTERNO), null, ErrorComun.ERROR_INTERNO.codigo());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(body).build();
    }
}
