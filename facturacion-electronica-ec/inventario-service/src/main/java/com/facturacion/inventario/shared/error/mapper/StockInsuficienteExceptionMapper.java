package com.facturacion.inventario.shared.error.mapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.facturacion.inventario.shared.error.ApiErrorResponse;
import com.facturacion.inventario.shared.exception.StockInsuficienteException;

@Provider
public class StockInsuficienteExceptionMapper implements ExceptionMapper<StockInsuficienteException> {

    @Override
    public Response toResponse(StockInsuficienteException exception) {
        ApiErrorResponse body = new ApiErrorResponse(409, "Conflict", exception.getMessage(), null);
        return Response.status(Response.Status.CONFLICT).entity(body).build();
    }
}
