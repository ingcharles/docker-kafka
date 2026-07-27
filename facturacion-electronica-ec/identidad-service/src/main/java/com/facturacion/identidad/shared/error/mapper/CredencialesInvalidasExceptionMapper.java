package com.facturacion.identidad.shared.error.mapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.facturacion.identidad.shared.error.ApiErrorResponse;
import com.facturacion.identidad.shared.exception.CredencialesInvalidasException;

@Provider
public class CredencialesInvalidasExceptionMapper implements ExceptionMapper<CredencialesInvalidasException> {

    @Override
    public Response toResponse(CredencialesInvalidasException exception) {
        ApiErrorResponse body = new ApiErrorResponse(401, "Unauthorized", exception.getMessage(), null);
        return Response.status(Response.Status.UNAUTHORIZED).entity(body).build();
    }
}
