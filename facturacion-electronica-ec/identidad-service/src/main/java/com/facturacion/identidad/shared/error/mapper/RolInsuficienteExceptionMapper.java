package com.facturacion.identidad.shared.error.mapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.facturacion.identidad.shared.error.ApiErrorResponse;
import com.facturacion.identidad.shared.exception.RolInsuficienteException;

@Provider
public class RolInsuficienteExceptionMapper implements ExceptionMapper<RolInsuficienteException> {

    @Override
    public Response toResponse(RolInsuficienteException exception) {
        ApiErrorResponse body = new ApiErrorResponse(403, "Forbidden", exception.getMessage(), null);
        return Response.status(Response.Status.FORBIDDEN).entity(body).build();
    }
}
