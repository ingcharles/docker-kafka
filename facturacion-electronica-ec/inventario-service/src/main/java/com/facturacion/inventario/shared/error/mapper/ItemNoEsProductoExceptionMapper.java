package com.facturacion.inventario.shared.error.mapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.facturacion.inventario.shared.error.ApiErrorResponse;
import com.facturacion.inventario.shared.exception.ItemNoEsProductoException;

@Provider
public class ItemNoEsProductoExceptionMapper implements ExceptionMapper<ItemNoEsProductoException> {

    @Override
    public Response toResponse(ItemNoEsProductoException exception) {
        ApiErrorResponse body = new ApiErrorResponse(400, "Bad Request", exception.getMessage(), null);
        return Response.status(Response.Status.BAD_REQUEST).entity(body).build();
    }
}
