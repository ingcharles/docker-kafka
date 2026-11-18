package com.facturacion.commons.error.mapper;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.facturacion.commons.error.ApiErrorResponse;
import com.facturacion.commons.error.ErrorComun;

/** Un argumento inválido (p. ej. un enum que no existe en un query param) es un error del cliente: 400. */
@Provider
public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

    @Override
    public Response toResponse(IllegalArgumentException exception) {
        ApiErrorResponse body = new ApiErrorResponse(400, "Bad Request", exception.getMessage(), null,
            ErrorComun.SOLICITUD_INVALIDA.codigo());
        return Response.status(Response.Status.BAD_REQUEST).entity(body).type(MediaType.APPLICATION_JSON).build();
    }
}
