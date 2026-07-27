package com.facturacion.comprobantes.shared.error.mapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.facturacion.comprobantes.shared.error.ApiErrorResponse;
import com.facturacion.comprobantes.shared.exception.TokenInvalidoException;

@Provider
public class TokenInvalidoExceptionMapper implements ExceptionMapper<TokenInvalidoException> {

    @Override
    public Response toResponse(TokenInvalidoException exception) {
        ApiErrorResponse body = new ApiErrorResponse(401, "Unauthorized", exception.getMessage(), null);
        return Response.status(Response.Status.UNAUTHORIZED).entity(body).build();
    }
}
