package com.facturacion.notificacion.shared.error.mapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.facturacion.notificacion.shared.error.ApiErrorResponse;
import com.facturacion.notificacion.shared.exception.NotFoundException;

@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

    @Override
    public Response toResponse(NotFoundException exception) {
        ApiErrorResponse body = new ApiErrorResponse(404, "Not Found", exception.getMessage(), null);
        return Response.status(Response.Status.NOT_FOUND).entity(body).build();
    }
}
