package com.facturacion.firma.shared.error.mapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.facturacion.firma.shared.error.ApiErrorResponse;
import com.facturacion.firma.shared.exception.CodigoDuplicadoException;

@Provider
public class CodigoDuplicadoExceptionMapper implements ExceptionMapper<CodigoDuplicadoException> {

    @Override
    public Response toResponse(CodigoDuplicadoException exception) {
        ApiErrorResponse body = new ApiErrorResponse(409, "Conflict", exception.getMessage(), null);
        return Response.status(Response.Status.CONFLICT).entity(body).build();
    }
}
