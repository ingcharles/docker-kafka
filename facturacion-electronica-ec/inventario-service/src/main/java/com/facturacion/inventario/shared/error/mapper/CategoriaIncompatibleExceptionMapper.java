package com.facturacion.inventario.shared.error.mapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.facturacion.inventario.shared.error.ApiErrorResponse;
import com.facturacion.inventario.shared.exception.CategoriaIncompatibleException;

@Provider
public class CategoriaIncompatibleExceptionMapper implements ExceptionMapper<CategoriaIncompatibleException> {

    @Override
    public Response toResponse(CategoriaIncompatibleException exception) {
        ApiErrorResponse body = new ApiErrorResponse(400, "Bad Request", exception.getMessage(), null);
        return Response.status(Response.Status.BAD_REQUEST).entity(body).build();
    }
}
