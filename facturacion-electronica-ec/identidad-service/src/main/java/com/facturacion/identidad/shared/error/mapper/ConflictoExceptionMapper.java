package com.facturacion.identidad.shared.error.mapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.facturacion.identidad.shared.error.ApiErrorResponse;
import com.facturacion.identidad.shared.exception.ConflictoException;

@Provider
public class ConflictoExceptionMapper implements ExceptionMapper<ConflictoException> {

    @Override
    public Response toResponse(ConflictoException exception) {
        ApiErrorResponse body = new ApiErrorResponse(409, "Conflict", exception.getMessage(), null);
        return Response.status(Response.Status.CONFLICT).entity(body).build();
    }
}
