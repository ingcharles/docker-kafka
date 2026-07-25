package com.facturacion.comprobantes.shared.error.mapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.facturacion.comprobantes.shared.error.ApiErrorResponse;
import com.facturacion.comprobantes.shared.exception.TipoComprobanteNoSoportadoException;

@Provider
public class TipoComprobanteNoSoportadoExceptionMapper implements ExceptionMapper<TipoComprobanteNoSoportadoException> {

    @Override
    public Response toResponse(TipoComprobanteNoSoportadoException exception) {
        ApiErrorResponse body = new ApiErrorResponse(400, "Bad Request", exception.getMessage(), null);
        return Response.status(Response.Status.BAD_REQUEST).entity(body).build();
    }
}
