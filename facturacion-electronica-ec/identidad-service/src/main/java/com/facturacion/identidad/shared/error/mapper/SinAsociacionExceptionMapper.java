package com.facturacion.identidad.shared.error.mapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.facturacion.identidad.shared.error.ApiErrorResponse;
import com.facturacion.identidad.shared.exception.SinAsociacionException;

@Provider
public class SinAsociacionExceptionMapper implements ExceptionMapper<SinAsociacionException> {

    @Override
    public Response toResponse(SinAsociacionException exception) {
        ApiErrorResponse body = new ApiErrorResponse(403, "Forbidden", exception.getMessage(), null);
        return Response.status(Response.Status.FORBIDDEN).entity(body).build();
    }
}
