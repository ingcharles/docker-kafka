package com.facturacion.inventario.shared.error.mapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.facturacion.inventario.shared.error.ApiErrorResponse;
import com.facturacion.inventario.shared.exception.SinPermisoException;

@Provider
public class SinPermisoExceptionMapper implements ExceptionMapper<SinPermisoException> {

    @Override
    public Response toResponse(SinPermisoException exception) {
        ApiErrorResponse body = new ApiErrorResponse(403, "Forbidden", exception.getMessage(), null);
        return Response.status(Response.Status.FORBIDDEN).entity(body).build();
    }
}
